package org.musicpimp

import android.app.Application
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import timber.log.Timber

class PimpApp: Application() {
    // https://developer.android.com/training/dependency-injection/manual
    private lateinit var pimpConf: PimpConf
    val conf: PimpConf
        get() = pimpConf
    override fun onCreate() {
        super.onCreate()
        val tree = if (BuildConfig.DEBUG) Timber.DebugTree() else NoLogging()
        Timber.plant(tree)

        AppCenter.start(this, "f7857cd4-6b66-42ba-b916-5a4382849a23",
            Analytics::class.java, Crashes::class.java)

        pimpConf = PimpConf(this.applicationContext)
    }

    class NoLogging: Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        }
    }
}
