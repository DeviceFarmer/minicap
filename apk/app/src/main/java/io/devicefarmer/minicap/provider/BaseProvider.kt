/*
 * Copyright (C) 2020 Orange
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

package io.devicefarmer.minicap.provider

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.net.LocalSocket
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import io.devicefarmer.minicap.output.DisplayOutput
import io.devicefarmer.minicap.output.MinicapClientOutput
import io.devicefarmer.minicap.SimpleServer
import io.devicefarmer.minicap.utils.DisplayManagerGlobal
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.io.PrintStream
import java.nio.ByteBuffer

/**
 * Base class to provide images of the screen. Those captures can be setup from SurfaceControl - as
 * it currently is - but could as well comes from MediaProjection API if useful in a future use case.
 * It basically receives screen images, do whatever processing needed (here, encodes in jpeg format)
 * and sends the results to an output (could be a file for screenshot, or a minicap client receiving the
 * jpeg stream)
 */
abstract class BaseProvider(protected val displayId: Int, private val targetSize: Size, val rotation: Int) : SimpleServer.Listener,
    ImageReader.OnImageAvailableListener {

    companion object {
        val log = LoggerFactory.getLogger(BaseProvider::class.java.simpleName)
    }

    /** Runs the image callbacks and the deferred sends, so they never run concurrently. */
    protected val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var clientOutput: DisplayOutput
    private lateinit var imageReader: ImageReader
    private val throttle = FrameThrottle()
    private var sendScheduled = false
    private val scheduledSend = Runnable {
        sendScheduled = false
        sendLatestImage(imageReader)
    }
    private var bitmap: Bitmap? = null //is used to compress the images

    var quality: Int = 100
    var frameRate: Float = Float.MAX_VALUE
        set(value) {
            throttle.periodMs = (1000 / value).toLong()
            log.info("framePeriodMs: ${throttle.periodMs}")
            field = value
        }

    abstract fun screenshot(printer: PrintStream)
    abstract fun getScreenSize(): Size

    fun getTargetSize(): Size = if(rotation%2 != 0) Size(targetSize.height, targetSize.width) else targetSize
    fun getImageReader(): ImageReader = imageReader

    fun init(out: DisplayOutput) {
        imageReader = ImageReader.newInstance(
            getTargetSize().width,
            getTargetSize().height,
            PixelFormat.RGBA_8888,
            2
        )
        clientOutput = out
    }

    override fun onConnection(socket: LocalSocket) {
        clientOutput = MinicapClientOutput(socket).apply {
            sendBanner(getScreenSize(),getTargetSize(),rotation)
        }
        init(clientOutput)
    }

    override fun onImageAvailable(reader: ImageReader) {
        // A scheduled send takes the latest image, so it also covers this one.
        if (sendScheduled) return
        val now = SystemClock.uptimeMillis()
        val sendAt = throttle.nextFrameAt(now)
        if (sendAt <= now) {
            sendLatestImage(reader)
        } else {
            // Too early for the frame rate. Leave the image in the reader and send the latest one
            // when the period ends, like the native minicap does. Dropping it would lose the last
            // frame before the screen goes static.
            log.debug("deferring frame by ${sendAt - now}ms")
            sendScheduled = true
            handler.postAtTime(scheduledSend, sendAt)
        }
    }

    private fun sendLatestImage(reader: ImageReader) {
        val image = reader.acquireLatestImage()
        if (image == null) {
            log.debug("no image available")
            return
        }
        image.use {
            throttle.frameSent(SystemClock.uptimeMillis())
            encode(it, quality, clientOutput.imageBuffer)
            clientOutput.send()
        }
    }

    private fun encode(image: Image, q: Int, out: OutputStream) {
        with(image) {
            val planes: Array<Image.Plane> = planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride: Int = planes[0].pixelStride
            val rowStride: Int = planes[0].rowStride
            val rowPadding: Int = rowStride - pixelStride * width
            // createBitmap can be resources consuming
            bitmap ?: Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            ).apply {
                copyPixelsFromBuffer(buffer)
            }.run {
                //the image need to be cropped
                Bitmap.createBitmap(this, 0, 0, getTargetSize().width, getTargetSize().height)
            }.apply {
                compress(Bitmap.CompressFormat.JPEG, q, out)
            }
        }
    }
}
