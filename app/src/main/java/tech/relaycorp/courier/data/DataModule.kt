package tech.relaycorp.courier.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import javax.inject.Named
import tech.relaycorp.courier.App
import tech.relaycorp.courier.data.database.AppDatabase

@Module
class DataModule {

    @Named("database_name")
    fun databaseName(appMode: App.Mode) =
        when (appMode) {
            App.Mode.Normal -> "courier"
            App.Mode.Test -> "courier_test"
        }

    fun database(context: Context, @Named("database_name") databaseName: String) =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
}
