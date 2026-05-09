package com.thebylito.navigationbarcolor

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import com.facebook.react.uimanager.IllegalViewOperationException

class NavigationBarColorModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {

    companion object {
        const val REACT_CLASS = "NavigationBarColor"
        private const val ERROR_NO_ACTIVITY = "E_NO_ACTIVITY"
        private const val ERROR_NO_ACTIVITY_MESSAGE = "Tried to change the navigation bar while not attached to an Activity"
        private const val ERROR_API_LEVEL = "API_LEVEl"
        private const val ERROR_API_LEVEL_MESSAGE = "Only Android Oreo and above is supported"
        private val UI_FLAG_HIDE_NAV_BAR = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun getName() = REACT_CLASS

    override fun getConstants(): Map<String, Any> = mapOf("EXAMPLE_CONSTANT" to "example")

    private fun setNavigationBarTheme(light: Boolean) {
        val activity = currentActivity ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val window = activity.window
            var flags = window.decorView.systemUiVisibility
            flags = if (light) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    @ReactMethod
    fun changeNavigationBarColor(color: String, light: Boolean, animated: Boolean, promise: Promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            promise.reject(ERROR_API_LEVEL, Throwable(ERROR_API_LEVEL_MESSAGE))
            return
        }
        val activity = currentActivity
        if (activity == null) {
            promise.reject(ERROR_NO_ACTIVITY, Throwable(ERROR_NO_ACTIVITY_MESSAGE))
            return
        }
        try {
            val window = activity.window
            runOnUiThread {
                if (color == "transparent" || color == "translucent") {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    if (color == "transparent") {
                        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    } else {
                        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    }
                    setNavigationBarTheme(light)
                    val map = Arguments.createMap()
                    map.putBoolean("success", true)
                    promise.resolve(map)
                    return@runOnUiThread
                }

                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

                val colorTo = Color.parseColor(color)
                if (animated) {
                    val colorFrom = window.navigationBarColor
                    ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
                        addUpdateListener { animator ->
                            window.navigationBarColor = animator.animatedValue as Int
                        }
                        start()
                    }
                } else {
                    window.navigationBarColor = colorTo
                }

                setNavigationBarTheme(light)
                val map = Arguments.createMap()
                map.putBoolean("success", true)
                promise.resolve(map)
            }
        } catch (e: IllegalViewOperationException) {
            val map = Arguments.createMap()
            map.putBoolean("success", false)
            promise.reject("error", e)
        }
    }

    @ReactMethod
    fun hideNavigationBar(promise: Promise) {
        try {
            runOnUiThread {
                currentActivity?.window?.decorView?.systemUiVisibility = UI_FLAG_HIDE_NAV_BAR
            }
        } catch (e: IllegalViewOperationException) {
            val map = Arguments.createMap()
            map.putBoolean("success", false)
            promise.reject("error", e)
        }
    }

    @ReactMethod
    fun showNavigationBar(promise: Promise) {
        try {
            runOnUiThread {
                currentActivity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        } catch (e: IllegalViewOperationException) {
            val map = Arguments.createMap()
            map.putBoolean("success", false)
            promise.reject("error", e)
        }
    }
}
