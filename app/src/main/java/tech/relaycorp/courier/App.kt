package tech.relaycorp.courier

import android.app.Application
import android.os.StrictMode
import tech.relaycorp.courier.common.di.AppComponent
import tech.relaycorp.courier.common.di.DaggerAppComponent
import timber.log.Timber

class App : Application() {

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }

    val mode by lazy {
        try {
            classLoader.loadClass("tech.relaycorp.courier.AppTest")
            Mode.Test
        } catch (e: ClassNotFoundException) {
            Mode.Normal
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupLogger()
        setupStrictMode()
    }

    private fun setupLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyDeath().build()
            )
            if (mode != Mode.Test) {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build()
                )
            }
        }
    }

    enum class Mode { Normal, Test }
}
