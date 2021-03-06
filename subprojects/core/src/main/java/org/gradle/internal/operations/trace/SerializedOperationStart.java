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

package org.gradle.internal.operations.trace;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.execution.internal.ExecuteTaskBuildOperationDetails;
import org.gradle.internal.logging.events.OperationIdentifier;
import org.gradle.internal.progress.BuildOperationDescriptor;
import org.gradle.internal.progress.OperationStartEvent;

import java.util.Collections;
import java.util.Map;

class SerializedOperationStart {

    final Object id;
    final Object parentId;
    final String displayName;

    final long startTime;

    final Object details;

    final Class<?> detailsType;

    SerializedOperationStart(BuildOperationDescriptor descriptor, OperationStartEvent startEvent) {
        this.id = ((OperationIdentifier) descriptor.getId()).getId();
        this.parentId = descriptor.getParentId() == null ? null : ((OperationIdentifier) descriptor.getParentId()).getId();
        this.displayName = descriptor.getDisplayName();
        this.startTime = startEvent.getStartTime();
        this.details = transform(descriptor.getDetails());
        this.detailsType = details == null ? null : descriptor.getDetails().getClass();
    }

    private Object transform(Object details) {
        if (details instanceof ExecuteTaskBuildOperationDetails) {
            ExecuteTaskBuildOperationDetails cast = (ExecuteTaskBuildOperationDetails) details;
            return Collections.singletonMap("task", cast.getTask().getPath());
        }

        return details;
    }

    SerializedOperationStart(Map<String, ?> map) throws ClassNotFoundException {
        this.id = map.get("id");
        this.parentId = map.get("parentId");
        this.displayName = (String) map.get("displayName");
        this.startTime = (Long) map.get("startTime");
        this.details = map.get("details");

        Object detailsTypeString = map.get("detailsType");
        if (detailsTypeString != null) {
            this.detailsType = getClass().getClassLoader().loadClass(detailsTypeString.toString());
        } else {
            this.detailsType = null;
        }
    }

    Map<String, ?> toMap() {
        ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();

        // Order is optimised for humans looking at the log.

        map.put("displayName", displayName);

        if (details != null) {
            map.put("details", details);
            map.put("detailsType", detailsType.getName());
        }

        map.put("id", id);
        if (parentId != null) {
            map.put("parentId", parentId);
        }
        map.put("startTime", startTime);

        return map.build();
    }

}
