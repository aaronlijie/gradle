/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.execution

import com.google.common.hash.HashCode
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.TaskOutputsInternal
import org.gradle.api.internal.changedetection.TaskArtifactState
import org.gradle.api.internal.tasks.SnapshotTaskInputsBuildOperationType
import org.gradle.api.internal.tasks.TaskExecuter
import org.gradle.api.internal.tasks.TaskExecutionContext
import org.gradle.api.internal.tasks.TaskStateInternal
import org.gradle.caching.internal.tasks.BuildCacheKeyInputs
import org.gradle.caching.internal.tasks.DefaultTaskOutputCachingBuildCacheKeyBuilder
import org.gradle.caching.internal.tasks.TaskOutputCachingBuildCacheKey
import org.gradle.caching.internal.tasks.TaskOutputCachingListener
import org.gradle.internal.operations.TestBuildOperationExecutor
import org.gradle.testing.internal.util.Specification
import org.gradle.util.Path

class ResolveBuildCacheKeyExecuterTest extends Specification {

    def taskState = Mock(TaskStateInternal)
    def task = Mock(TaskInternal)
    def taskContext = Mock(TaskExecutionContext)
    def taskArtifactState = Mock(TaskArtifactState)
    def taskOutputs = Mock(TaskOutputsInternal)
    def delegate = Mock(TaskExecuter)
    def listener = Mock(TaskOutputCachingListener)
    def buildOperationExecutor = new TestBuildOperationExecutor()
    def executer = new ResolveBuildCacheKeyExecuter(listener, delegate, buildOperationExecutor)
    def cacheKey = Mock(TaskOutputCachingBuildCacheKey)

    def "notifies listener after calculating cache key"() {
        when:
        executer.execute(task, taskState, taskContext)

        then:
        with(buildOpDetails()) {
            taskPath == ":foo"
            taskId != 0
        }

        with(buildOpResult(), ResolveBuildCacheKeyExecuter.OperationResultImpl) {
            key == cacheKey
        }

        then:
        1 * task.getIdentityPath() >> Path.path(":foo")
        1 * taskContext.getTaskArtifactState() >> taskArtifactState
        1 * taskArtifactState.calculateCacheKey() >> cacheKey

        then:
        1 * task.getOutputs() >> taskOutputs
        1 * taskOutputs.getHasOutput() >> true
        1 * listener.cacheKeyEvaluated(task, cacheKey)
        1 * cacheKey.isValid() >> true
        1 * cacheKey.getHashCode() >> "0123456789abcdef"

        then:
        1 * taskContext.setBuildCacheKey(cacheKey)

        then:
        1 * delegate.execute(task, taskState, taskContext)
        1 * task.getIdentityPath() >> Path.path(":foo")
        0 * _
    }

    def "propagates exceptions if cache key cannot be calculated"() {
        def failure = new RuntimeException("Bad cache key")

        when:
        executer.execute(task, taskState, taskContext)

        then:
        1 * task.getIdentityPath() >> Path.path(":foo")
        1 * taskContext.getTaskArtifactState() >> taskArtifactState
        1 * taskArtifactState.calculateCacheKey() >> {
            throw failure
        }
        0 * _

        def ex = thrown RuntimeException
        ex.is(failure)
        buildOpFailure().is(failure)
    }

    def "does not call listener if task has no outputs"() {
        when:
        executer.execute(task, taskState, taskContext)

        then:
        1 * task.getIdentityPath() >> Path.path(":foo")
        1 * taskContext.getTaskArtifactState() >> taskArtifactState
        1 * taskArtifactState.calculateCacheKey() >> DefaultTaskOutputCachingBuildCacheKeyBuilder.NO_CACHE_KEY

        then:
        1 * task.getOutputs() >> taskOutputs
        1 * taskOutputs.getHasOutput() >> false

        then:
        1 * taskContext.setBuildCacheKey(DefaultTaskOutputCachingBuildCacheKeyBuilder.NO_CACHE_KEY)

        then:
        1 * delegate.execute(task, taskState, taskContext)
        0 * _

        and:
        with(buildOpResult(), ResolveBuildCacheKeyExecuter.OperationResultImpl) {
            key == DefaultTaskOutputCachingBuildCacheKeyBuilder.NO_CACHE_KEY
        }
    }

    def "adapts key to result interface"() {
        given:
        def inputs = Mock(BuildCacheKeyInputs)
        def key = Mock(TaskOutputCachingBuildCacheKey) {
            getInputs() >> inputs
        }
        def adapter = new ResolveBuildCacheKeyExecuter.OperationResultImpl(key)

        when:
        inputs.inputHashes >> [b: HashCode.fromString("bb"), a: HashCode.fromString("aa")]

        then:
        adapter.inputHashes == [a: "aa", b: "bb"]

        when:
        inputs.classLoaderHash >> HashCode.fromString("cc")

        then:
        adapter.classLoaderHash == "cc"

        when:
        inputs.actionClassLoaderHashes >> [HashCode.fromString("ee"), HashCode.fromString("dd")]

        then:
        adapter.actionClassLoaderHashes == ["ee", "dd"]

        when:
        inputs.outputPropertyNames >> ["2", "1"].toSet()

        then:
        adapter.outputPropertyNames == new TreeSet(["1", "2"])

        when:
        key.hashCode >> HashCode.fromString("ff")

        then:
        adapter.buildCacheKey == "ff"
    }

    private SnapshotTaskInputsBuildOperationType.Details buildOpDetails() {
        buildOperationExecutor.log.mostRecentDetails(SnapshotTaskInputsBuildOperationType)
    }

    private SnapshotTaskInputsBuildOperationType.Result buildOpResult() {
        buildOperationExecutor.log.mostRecentResult(SnapshotTaskInputsBuildOperationType)
    }

    private Throwable buildOpFailure() {
        buildOperationExecutor.log.mostRecentFailure(SnapshotTaskInputsBuildOperationType)
    }

}
