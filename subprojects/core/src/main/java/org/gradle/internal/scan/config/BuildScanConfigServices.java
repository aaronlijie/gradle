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

package org.gradle.internal.scan.config;

import org.gradle.StartParameter;
import org.gradle.internal.event.ListenerManager;
import org.gradle.internal.scan.BuildScanRequest;

/**
 * Wiring of the objects that provide the build scan config integration.
 *
 * This is build scoped.
 */
public class BuildScanConfigServices {

    BuildScanPluginCompatibilityEnforcer createBuildScanPluginCompatibilityEnforcer() {
        return BuildScanPluginCompatibilityEnforcer.create();
    }

    BuildScanConfigManager createBuildScanConfigManager(StartParameter startParameter, ListenerManager listenerManager, BuildScanPluginCompatibilityEnforcer compatibilityEnforcer) {
        return new BuildScanConfigManager(startParameter, listenerManager, compatibilityEnforcer);
    }

    // legacy support
    BuildScanRequest createBuildScanRequest(BuildScanPluginCompatibilityEnforcer compatibilityEnforcer) {
        return new BuildScanRequestLegacyBridge(compatibilityEnforcer);
    }

}
