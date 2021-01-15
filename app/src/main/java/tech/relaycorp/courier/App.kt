package tech.relaycorp.courier

import android.app.Application
import android.os.Build
import android.os.StrictMode
import tech.relaycorp.courier.background.WifiHotspotStateReceiver
import tech.relaycorp.courier.common.Logging
import tech.relaycorp.courier.common.di.AppComponent
import tech.relaycorp.courier.common.di.DaggerAppComponent
import java.util.logging.Level
import java.util.logging.LogManager
import javax.inject.Inject

open class App : Application() {

    @Inject
    lateinit var wifiHotspotStateReceiver: WifiHotspotStateReceiver

    open val component: AppComponent by lazy {
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
        component.inject(this)
        setupLogger()
        setupStrictMode()
        wifiHotspotStateReceiver.register()
    }

    override fun onTerminate() {
        super.onTerminate()
        wifiHotspotStateReceiver.unregister()
    }

    private fun setupLogger() {
        LogManager.getLogManager()
        Logging.level = if (BuildConfig.DEBUG) Level.ALL else Level.WARNING
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG && mode != Mode.Test) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build()
            )
            StrictMode.setVmPolicy(
                /*
                  To disable the some of the checks we need to manually set all checks.
                  This code is based on the `detectAll()` implementation.
                  Checks disabled:
                  - UntaggedSockets (we aren't able to tag Netty socket threads)
                  - CleartextNetwork (it's considering gRPC over TLS communication as cleartext)
                  - ActivityLeaks (it's too eager on Android 5, triggering unnecessarily)
                 */
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectFileUriExposure()
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            detectContentUriWithoutPermission()
                        }
                    }
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            detectCredentialProtectedWhileLocked()
                        }
                    }
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
    }

    enum class Mode { Normal, Test }
}
