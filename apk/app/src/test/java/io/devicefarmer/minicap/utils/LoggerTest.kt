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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LoggerTest {
    private val time = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 19, 9, 5, 7)
        set(Calendar.MILLISECOND, 42)
    }.time

    @Test
    fun formatsLikeLogback() {
        assertEquals("09:05:07.042 [main] INFO  Main - Listening on socket : minicap\n",
            Logger.format(time, "main", "INFO", "Main", "Listening on socket : minicap", null))
    }

    // STF fails the device worker on a line containing ERROR, so the level word must stay.
    @Test
    fun keepsTheErrorLevelWord() {
        assertEquals("09:05:07.042 [main] ERROR SurfaceControl - SurfaceControl error\n",
            Logger.format(time, "main", "ERROR", "SurfaceControl", "SurfaceControl error", null))
    }

    @Test
    fun appendsTheStackTrace() {
        val text = Logger.format(time, "main", "WARN", "SurfaceProvider", "falling back",
            IllegalStateException("no display"))
        assertTrue(text.startsWith("09:05:07.042 [main] WARN  SurfaceProvider - falling back\n" +
            "java.lang.IllegalStateException: no display\n\tat "))
    }
}
