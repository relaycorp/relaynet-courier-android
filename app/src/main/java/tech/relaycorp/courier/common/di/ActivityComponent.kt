package tech.relaycorp.courier.common.di

import dagger.Subcomponent
import tech.relaycorp.courier.ui.main.MainActivity

@PerActivity
@Subcomponent
interface ActivityComponent {
    // Activities
    fun inject(activity: MainActivity)
}
