package me.masm11.contextplayer.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

class Log {
    companion object {
	fun d(str: String, e: Throwable? = null) {
	    android.util.Log.d("ContextPlayer", str)
	}
	fun i(str: String, e: Throwable? = null) {
	    android.util.Log.i("ContextPlayer", str)
	}
	fun w(str: String, e: Throwable? = null) {
	    android.util.Log.w("ContextPlayer", str)
	}
	fun e(str: String, e: Throwable) {
	    android.util.Log.e("ContextPlayer", str, e)
	    val crashlytics = FirebaseCrashlytics.getInstance()
	    crashlytics.recordException(e)
	}
	fun init() {
	    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
	}
    }
}