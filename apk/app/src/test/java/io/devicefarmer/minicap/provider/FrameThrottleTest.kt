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

package io.devicefarmer.minicap.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameThrottleTest {

    @Test
    fun `sends the first frame right away`() {
        assertEquals(1_000L, FrameThrottle(500).nextFrameAt(1_000))
    }

    @Test
    fun `sends every frame right away without a frame rate`() {
        val throttle = FrameThrottle()
        throttle.frameSent(1_000)

        assertEquals(1_000L, throttle.nextFrameAt(1_000))
    }

    @Test
    fun `defers a frame that comes too early to the end of the period`() {
        val throttle = FrameThrottle(500)
        throttle.frameSent(1_000)

        assertEquals(1_500L, throttle.nextFrameAt(1_100))
    }

    @Test
    fun `sends a frame right away once the period has passed`() {
        val throttle = FrameThrottle(500)
        throttle.frameSent(1_000)

        assertEquals(1_500L, throttle.nextFrameAt(1_500))
        assertEquals(2_000L, throttle.nextFrameAt(2_000))
    }

    @Test
    fun `measures the next period from the last frame sent`() {
        val throttle = FrameThrottle(500)
        throttle.frameSent(1_000)
        throttle.frameSent(1_520)

        assertEquals(2_020L, throttle.nextFrameAt(1_600))
    }

    @Test
    fun `applies a new frame rate to the next frame`() {
        val throttle = FrameThrottle(500)
        throttle.frameSent(1_000)
        throttle.periodMs = 100

        assertEquals(1_100L, throttle.nextFrameAt(1_050))
    }
}
