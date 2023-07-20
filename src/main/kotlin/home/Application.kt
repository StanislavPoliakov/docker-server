package home

import home.models.DaoImpl
import home.plugins.*
import home.probes.Probe
import io.ktor.server.application.*

val probe = Probe

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    configureDatabase()
    val database = DaoImpl()
    configureAdministration()
    configureSerialization(probe, database)
    configureHTTP()
    configureRouting()

}
