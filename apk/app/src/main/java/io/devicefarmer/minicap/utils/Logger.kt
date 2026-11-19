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

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes each message to logcat and to stderr. Nothing goes to stdout, which carries the -s
 * screenshot and the -i display info.
 *
 * The stderr lines keep the format logback printed. STF reads them through adb shell, takes
 * the process ID from the "PID:" line and treats any line containing "ERROR" as fatal, so the
 * level must stay in each line. Debug messages are off, as they were in logback.xml.
 */
class Logger(private val tag: String) {

    fun info(message: String) = log(Log.INFO, "INFO", message, null)

    fun warn(message: String, error: Throwable? = null) = log(Log.WARN, "WARN", message, error)

    fun error(message: String, error: Throwable? = null) = log(Log.ERROR, "ERROR", message, error)

    @Suppress("UNUSED_PARAMETER")
    inline fun debug(message: () -> String) {
    }

    private fun log(priority: Int, level: String, message: String, error: Throwable?) {
        Log.println(priority, tag,
            if (error == null) message else message + "\n" + Log.getStackTraceString(error))
        System.err.print(format(Date(), Thread.currentThread().name, level, tag, message, error))
    }

    companion object {
        /** Same layout as logback's `%d{HH:mm:ss.SSS} [%thread] %-5level %logger - %msg%n`. */
        fun format(time: Date, thread: String, level: String, tag: String, message: String,
                   error: Throwable?): String {
            val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(time)
            val line = "$clock [$thread] ${level.padEnd(5)} $tag - $message\n"
            return if (error == null) line else line + error.stackTraceToString()
        }
    }
}
