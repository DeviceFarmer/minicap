/*
 * Copyright (C) 2020 Orange
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

import android.graphics.Rect
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.net.LocalSocket
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Size
import io.devicefarmer.minicap.output.ScreenshotOutput
import io.devicefarmer.minicap.utils.DisplayInfo
import io.devicefarmer.minicap.utils.DisplayManager
import io.devicefarmer.minicap.utils.DisplayManagerGlobal
import io.devicefarmer.minicap.utils.SurfaceControl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintStream
import kotlin.system.exitProcess

/**
 * Provides screen images by mirroring the display into an [ImageReader]. This is pretty similar
 * to the native version of minicap but here it is done at a higher level making things a bit
 * easier.
 */
class SurfaceProvider(private val displayId: Int, targetSize: Size, orientation: Int) : BaseProvider(displayId, targetSize, orientation) {
    constructor(display: Int) : this(display, currentScreenSize(), currentRotation())

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SurfaceProvider::class.java.simpleName)

        private fun currentScreenSize(): Size {
            return currentDisplayInfo().run {
                Size(this.size.width, this.size.height)
            }
        }

        private fun currentRotation(): Int = currentDisplayInfo().rotation

        private fun currentDisplayInfo(): DisplayInfo {
            return DisplayManagerGlobal.getDisplayInfo(0)
        }
    }

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var display: IBinder? = null
    private var virtualDisplay: VirtualDisplay? = null

    val displayInfo: DisplayInfo = DisplayManagerGlobal.getDisplayInfo(displayId)

    override fun getScreenSize(): Size = displayInfo.size


    override fun screenshot(printer: PrintStream) {
        init(ScreenshotOutput(printer))
        initSurface {
            super.onImageAvailable(it)
            exitProcess(0)
        }
    }

    /**
     *
     */
    override fun onConnection(socket: LocalSocket) {
        super.onConnection(socket)
        initSurface()
    }

    /**
     * Setup the Surface between the display and an ImageReader so that we can grab the
     * screen. Like scrcpy, the API is chosen by SDK level: `SurfaceControl.createDisplay`
     * is gone from Android 15, which mirrors through a virtual display instead.
     */
    private fun initSurface(l: ImageReader.OnImageAvailableListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            initVirtualDisplay()
        } else {
            initSurfaceControlDisplay()
        }
        getImageReader().setOnImageAvailableListener(l, handler)
    }

    // Up to Android 14.
    private fun initSurfaceControlDisplay() {
        //must be done on the main thread
        // Support  Android 12 (preview),and resolve black screen problem
        val secure = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Build.VERSION.SDK_INT == Build.VERSION_CODES.R && "S" != Build.VERSION.CODENAME
        display = SurfaceControl.createDisplay("minicap", secure)
        //initialise the surface to get the display in the ImageReader
        SurfaceControl.openTransaction()
        try {
            SurfaceControl.setDisplaySurface(display, getImageReader().surface)
            SurfaceControl.setDisplayProjection(display, 0, Rect(0, 0, getScreenSize().width, getScreenSize().height), Rect(0, 0, getTargetSize().width, getTargetSize().height))
            SurfaceControl.setDisplayLayerStack(display, displayInfo.layerStack)
        } finally {
            SurfaceControl.closeTransaction()
        }
    }

    // Android 15 and later.
    private fun initVirtualDisplay() {
        try {
            // The virtual display has the size of the ImageReader, and the mirrored display is
            // scaled into it.
            virtualDisplay = DisplayManager.create().createVirtualDisplay(
                "minicap", getTargetSize().width, getTargetSize().height, displayId, getImageReader().surface
            )
        } catch (e: Exception) {
            log.error("could not mirror display $displayId into a virtual display", e)
            exitProcess(1)
        }
    }

    private fun initSurface() {
        initSurface(this)
    }
}
