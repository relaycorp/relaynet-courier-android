package tech.relaycorp.courier.common.di

import dagger.Subcomponent
import tech.relaycorp.courier.ui.main.MainActivity
import tech.relaycorp.courier.ui.sync.internet.InternetSyncActivity

@PerActivity
@Subcomponent
interface ActivityComponent {

    // Activities

    fun inject(activity: InternetSyncActivity)
    fun inject(activity: MainActivity)
}
