package tech.relaycorp.courier.test

import androidx.test.platform.app.InstrumentationRegistry

// App

val context get() = InstrumentationRegistry.getInstrumentation().targetContext
val app get() = context.applicationContext as TestApp
val appComponent get() = app.component
