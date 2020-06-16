package tech.relaycorp.courier.test

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import tech.relaycorp.courier.data.database.AppDatabase
import javax.inject.Inject

class ClearTestDatabaseRule : TestRule {

    @Inject
    lateinit var database: AppDatabase

    override fun apply(base: Statement, description: Description?) =
        object : Statement() {
            override fun evaluate() {
                appComponent.inject(this@ClearTestDatabaseRule)
                database.clearAllTables()
                base.evaluate()
                database.clearAllTables()
            }
        }
}
