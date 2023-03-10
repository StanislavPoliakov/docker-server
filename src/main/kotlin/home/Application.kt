package home

import home.plugins.configureAdministration
import home.plugins.configureHTTP
import home.plugins.configureRouting
import home.plugins.configureSerialization
import io.ktor.server.application.Application
import home.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    configureAdministration()
    configureSerialization()
    configureHTTP()
    configureRouting()
}
