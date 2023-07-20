package home.plugins

import home.models.UserTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

fun Application.configureDatabase() {
    val database = environment.config.propertyOrNull("ktor.database.name")?.getString().takeUnless { it.isNullOrEmpty() }?.let { dbName ->
        val jdbcUrl = "jdbc:postgresql://database-postgresql:5432/$dbName"
        val driver = environment.config.property("ktor.database.driver_postgres").getString()
        val username = environment.config.property("ktor.database.username").getString()
        val password = environment.config.property("ktor.database.password").getString()

        Database.connect(url = jdbcUrl, driver = driver, user = username, password = password)
    } ?: run {
        val jdbcUrl = environment.config.property("ktor.database.jdbcLocalURL").getString()

        Database.connect({ DriverManager.getConnection("$jdbcUrl;MODE=MySQL") })
    }
    transaction(database) {
        SchemaUtils.create(UserTable)
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }