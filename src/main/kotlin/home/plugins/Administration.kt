package home.plugins

import io.ktor.server.engine.*
import io.ktor.server.application.*

fun Application.configureAdministration() {
    install(ShutDownUrl.ApplicationCallPlugin) {
        exitCodeSupplier = { 0 } // ApplicationCall.() -> Int
    }
}
