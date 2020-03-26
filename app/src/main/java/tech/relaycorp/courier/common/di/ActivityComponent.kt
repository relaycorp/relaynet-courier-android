package tech.relaycorp.courier.common.di

import dagger.Subcomponent
import tech.relaycorp.courier.ui.main.MainActivity
import tech.relaycorp.courier.ui.sync.internet.InternetSyncActivity
import tech.relaycorp.courier.ui.sync.people.HotspotInstructionsActivity
import tech.relaycorp.courier.ui.sync.people.PeopleSyncActivity

@PerActivity
@Subcomponent
interface ActivityComponent {

    // Activities

    fun inject(activity: HotspotInstructionsActivity)
    fun inject(activity: InternetSyncActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: PeopleSyncActivity)
}
