package home.plugins

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.application.*

fun Application.configureRouting() {
    val host = environment.config.host
    val port = environment.config.port
    install(StatusPages) {
        status(HttpStatusCode.Gone) { call, status ->
            call.respondText(text = "Server [$host:$port] was shut down!", status = status)
            call.application.environment.log.debug("Server [$host:$port] was shut down!")
        }
    }
}
