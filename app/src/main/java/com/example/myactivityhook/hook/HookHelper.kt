package com.example.myactivityhook.hook

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.Q
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.example.myactivityhook.StubActivity
import java.lang.reflect.Proxy
import java.util.*

object HookHelper {
    private const val TAG = "Ying"
    const val EXTRA_TARGET_INTENT = "extra_target_intent"

    //hook之后，就一直存在，如何关闭hook？
    fun hookInstrumentation(activity: Activity) {
        val activityClass: Class<*> = Activity::class.java
        val field = activityClass.getDeclaredField("mInstrumentation")
        field.isAccessible = true
        val instrumentation = field.get(activity) as Instrumentation
        val instrumentationProxy = ProxyInstrumentation(instrumentation)
        field.set(activity, instrumentationProxy)
    }

    class ProxyInstrumentation(private val mBase: Instrumentation) : Instrumentation() {

//        fun execStartActivity(
//            who: Context, contextThread: IBinder, token: IBinder, target: Activity,
//            intent: Intent, requestCode: Int, options: Bundle?
//        ): ActivityResult? {
//            Log.e(
//                TAG, "执行了startActivity, 参数如下: " + "who = [" + who + "], " +
//                        "contextThread = [" + contextThread + "], token = [" + token + "], " +
//                        "target = [" + target + "], intent = [" + intent +
//                        "], requestCode = [" + requestCode + "], options = [" + options + "]"
//            )
//            val instrumentationClass: Class<*> = Instrumentation::class.java
//            val execStartActivity = instrumentationClass.getDeclaredMethod(
//                "execStartActivity",
//                Context::class.java,
//                IBinder::class.java,
//                IBinder::class.java,
//                Activity::class.java,
//                Intent::class.java,
//                Int::class.javaPrimitiveType,
//                Bundle::class.java
//            )
//            execStartActivity.isAccessible = true
//
//
//
//            return execStartActivity.invoke(
//                mBase, who,
//                contextThread, token, target, intent, requestCode, options
//            ) as ActivityResult?
//        }

        fun execStartActivity(
            who: Context, contextThread: IBinder, token: IBinder?, target: Activity?,
            intent: Intent, requestCode: Int, options: Bundle?
        ): ActivityResult? {
            Log.e(
                TAG, "执行了startActivity, 参数如下: " + "who = [" + who + "], " +
                        "contextThread = [" + contextThread + "], token = [" + token + "], " +
                        "target = [" + target + "], intent = [" + intent +
                        "], requestCode = [" + requestCode + "], options = [" + options + "]"
            )
            val instrumentationClass: Class<*> = Instrumentation::class.java
            val execStartActivity = instrumentationClass.getDeclaredMethod(
                "execStartActivity",
                Context::class.java,
                IBinder::class.java,
                IBinder::class.java,
                Activity::class.java,
                Intent::class.java,
                Int::class.javaPrimitiveType,
                Bundle::class.java
            )
            execStartActivity.isAccessible = true



            return execStartActivity.invoke(
                mBase, who,
                contextThread, token, target, intent, requestCode, options
            ) as ActivityResult?
        }

        override fun newActivity(cl: ClassLoader?, className: String?, intent: Intent?): Activity {
            return mBase.newActivity(cl, className, intent)
        }
    }

    //无法打印instrumentationProxy的内容
    fun hookActivityThreadInstrumentation() {
        val atClass: Class<*> = Class.forName("android.app.ActivityThread")
        val instrumentationField = atClass.getDeclaredField("mInstrumentation")
        instrumentationField.isAccessible = true
        val currentActivityThreadMethod = atClass.getDeclaredMethod("currentActivityThread")
        currentActivityThreadMethod.isAccessible = true
        val currentActivityThread = currentActivityThreadMethod.invoke(null)
        val instrumentation = instrumentationField.get(currentActivityThread) as Instrumentation
        val instrumentationProxy = ProxyInstrumentation(instrumentation)
        instrumentationField.set(currentActivityThread, instrumentationProxy)
    }

    fun hookAMS() {
        val gDefaultField = when (SDK_INT) {
            Q -> {
                val atmClass: Class<*> = Class.forName("android.app.ActivityTaskManager")
                atmClass.getDeclaredField("IActivityTaskManagerSingleton")
            }
            in O until Q -> {
                val activityManagerClass: Class<*> = Class.forName("android.app.ActivityManager")
                activityManagerClass.getDeclaredField("IActivityManagerSingleton")
            }
            else -> {
                val activityManagerClass: Class<*> =
                    Class.forName("android.app.ActivityManagerNative")
                activityManagerClass.getDeclaredField("gDefault");
            }
        }
        gDefaultField.isAccessible = true
        val gDefault = gDefaultField.get(null)
        val singletonClass = Class.forName("android.util.Singleton")
        val mInstanceField = singletonClass.getDeclaredField("mInstance")
        mInstanceField.isAccessible = true
        val am = mInstanceField.get(gDefault)
        val amClass = if (SDK_INT == Q) {
            Class.forName("android.app.IActivityTaskManager")
        } else {
            Class.forName("android.app.IActivityManager")
        }
        val proxy = Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(amClass)
        ) { proxy, method, args ->
            if (method.name == "startActivity") {
                for (arg in args) {
                    Log.e(TAG, "arg:$arg")
                }
            }
            return@newProxyInstance method.invoke(am, *args)
        }
        mInstanceField.set(gDefault, proxy)

    }

    //放到onCreate中可以，要不只能第一次成功
    fun hookIActivityManager() {
        val gDefaultField = when (SDK_INT) {
            Q -> {
                val atmClass: Class<*> = Class.forName("android.app.ActivityTaskManager")
                atmClass.getDeclaredField("IActivityTaskManagerSingleton")
            }
            in O until Q -> {
                val activityManagerClass: Class<*> = Class.forName("android.app.ActivityManager")
                activityManagerClass.getDeclaredField("IActivityManagerSingleton")
            }
            else -> {
                val activityManagerClass: Class<*> =
                    Class.forName("android.app.ActivityManagerNative")
                activityManagerClass.getDeclaredField("gDefault");
            }
        }
        gDefaultField.isAccessible = true
        val gDefault = gDefaultField.get(null)
        val singletonClass = Class.forName("android.util.Singleton")
        val mInstanceField = singletonClass.getDeclaredField("mInstance")
        mInstanceField.isAccessible = true
        val rawIActivityManager = mInstanceField.get(gDefault)
        val classLoader = Thread.currentThread().contextClassLoader
        val iActivityManagerInterface = if (SDK_INT == Q) {
            Class.forName("android.app.IActivityTaskManager")
        } else {
            Class.forName("android.app.IActivityManager")
        }
        val proxy = Proxy.newProxyInstance(
            classLoader,
            arrayOf(iActivityManagerInterface)
        ) { proxy, method, args ->
            if (method.name == "startActivity") {
                for (i in args.indices) {
                    if (args[i] is Intent) {
                        val newIntent = Intent()
                        val subPackage = "com.example.myactivityhook"
                        newIntent.component =
                            ComponentName(subPackage, StubActivity::class.java.name)
                        newIntent.putExtra(EXTRA_TARGET_INTENT, args[i] as Intent)
                        args[i] = newIntent
                        break
                    }
                }
            }
            return@newProxyInstance method.invoke(rawIActivityManager, *args)
        }
        mInstanceField.set(gDefault, proxy)
    }

    fun hookHandler() {
        val atClass: Class<*> = Class.forName("android.app.ActivityThread")
        val sCurrentActivityThreadField = atClass.getDeclaredField("sCurrentActivityThread")
        sCurrentActivityThreadField.isAccessible = true
        val sCurrentActivityThread = sCurrentActivityThreadField.get(null)
        val mHField = atClass.getDeclaredField("mH")
        mHField.isAccessible = true

        val mH = mHField.get(sCurrentActivityThread) as Handler

        val mCallbackField = Handler::class.java.getDeclaredField("mCallback")
        mCallbackField.isAccessible = true
        mCallbackField.set(mH, Handler.Callback {
            Log.e(TAG, "handleMessage:" + it.what)
            when (it.what) {
                100 -> {
                    val intentField = it.obj.javaClass.getDeclaredField("intent")
                    intentField.isAccessible = true
                    val intent = intentField.get(it.obj) as Intent
                    val targetIntent = intent.getParcelableExtra<Intent>(EXTRA_TARGET_INTENT)
                    intent.component = targetIntent.component
                }
                159 -> {
                    val mActivityCallbacksField =
                        it.obj.javaClass.getDeclaredField("mActivityCallbacks")
                    mActivityCallbacksField.isAccessible = true
                    val mActivityCallbacks = mActivityCallbacksField.get(it.obj) as List<Object>
                    if (mActivityCallbacks.isNotEmpty()) {
                        val className = "android.app.servertransaction.LaunchActivityItem"
                        if (mActivityCallbacks[0].javaClass.canonicalName == className) {
                            val obj = mActivityCallbacks[0]
                            val intentField = obj.javaClass.getDeclaredField("mIntent")
                            intentField.isAccessible = true
                            val intent = intentField.get(obj) as Intent
                            intent.component =
                                (intent.getParcelableExtra(EXTRA_TARGET_INTENT) as Intent).component
                        }
                    }

                }
            }
            mH.handleMessage(it)
            return@Callback true
        })


    }
}


