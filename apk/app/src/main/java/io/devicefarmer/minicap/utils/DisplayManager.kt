package io.devicefarmer.minicap.utils

import android.annotation.SuppressLint
import android.hardware.display.VirtualDisplay
import android.view.Display
import android.view.Surface
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.regex.Pattern

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
class DisplayManager private constructor(private val manager: Any) {

    private var createVirtualDisplayMethod: Method? = null

    companion object {
        fun create(): DisplayManager {
            try {
                val clazz = Class.forName("android.hardware.display.DisplayManagerGlobal")
                val getInstanceMethod = clazz.getDeclaredMethod("getInstance")
                val dmg = getInstanceMethod.invoke(null)
                return DisplayManager(dmg)
            } catch (e: ReflectiveOperationException) {
                throw AssertionError(e)
            }
        }
    }

    private fun parseDisplayFlags(text: String?): Int {
        val regex = Pattern.compile("FLAG_[A-Z_]+")
        var flags = 0
        if (text == null) return flags
        val m = regex.matcher(text)
        while (m.find()) {
            val flagString = m.group()
            try {
                val filed: Field = Display::class.java.getDeclaredField(flagString)
                flags = flags or filed.getInt(null)
            } catch (e: ReflectiveOperationException) {
                // Silently ignore, some flags reported by "dumpsys display" are @TestApi
            }
        }
        return flags
    }

    fun getDisplayIds(): IntArray {
        return try {
            manager.javaClass.getMethod("getDisplayIds").invoke(manager) as IntArray
        } catch (e: ReflectiveOperationException) {
            throw AssertionError(e)
        }
    }

    @Throws(NoSuchMethodException::class)
    private fun getCreateVirtualDisplayMethod(): Method {
        if (createVirtualDisplayMethod == null) {
            createVirtualDisplayMethod = android.hardware.display.DisplayManager::class.java
                .getMethod("createVirtualDisplay", String::class.java, Int::class.java, Int::class.java, Int::class.java, Surface::class.java)
        }
        return createVirtualDisplayMethod!!
    }

    @Throws(Exception::class)
    fun createVirtualDisplay(name: String, width: Int, height: Int, displayIdToMirror: Int, surface: Surface): VirtualDisplay {
        val method: Method = getCreateVirtualDisplayMethod()
        return method.invoke(null, name, width, height, displayIdToMirror, surface) as VirtualDisplay
    }
}


