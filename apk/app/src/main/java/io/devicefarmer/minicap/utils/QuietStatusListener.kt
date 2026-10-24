/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.devicefarmer.minicap.utils

import ch.qos.logback.core.status.Status
import ch.qos.logback.core.status.StatusListener

/**
 * Drops logback's own status messages. Registering any status listener stops logback from
 * printing them to stdout, which would corrupt the screenshot written there.
 */
class QuietStatusListener : StatusListener {
    override fun addStatusEvent(status: Status) {}
}
