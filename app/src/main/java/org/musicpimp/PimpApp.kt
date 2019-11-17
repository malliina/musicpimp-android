package org.musicpimp

import android.app.Application
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import timber.log.Timber

class PimpApp: Application() {
    override fun onCreate() {
        super.onCreate()
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)

//        AppCenter.start(this, "768ec01e-fe9c-46b2-a05a-5389fa9d148f",
//            Analytics::class.java, Crashes::class.java)
    }

    class NoLogging: Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        }
    }
}
