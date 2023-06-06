package home.plugins

import home.probes.Probe
import home.probes.ProbeStatus
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSerialization(probe: Probe) {
    install(ContentNegotiation) {
        json()
    }
    val hostname = System.getenv()["HOSTNAME"] ?: "local machine"
    routing {
        get("/health/") {
            call.respond(status = HttpStatusCode.OK, message = mapOf("status" to "OK"))
            }

        get("/check/") {
            call.respond(status = HttpStatusCode.OK, message = "-> ALIVE from: $hostname")
        }

        get("/probe/liveness/") {
            val statusCode = when (probe.liveness) {
                ProbeStatus.SUCCESS -> HttpStatusCode.OK
                ProbeStatus.FAILED -> HttpStatusCode.ServiceUnavailable
            }
            call.respond(status = statusCode, message = "${probe.liveness}")
        }

        get("/probe/readiness/") {
            val statusCode = when (probe.readiness) {
                ProbeStatus.SUCCESS -> HttpStatusCode.OK
                ProbeStatus.FAILED -> HttpStatusCode.ServiceUnavailable
            }
            call.respond(status = statusCode, message = "${probe.readiness}")
        }

        get("/probe/all/") {
            call.respond(
                status = HttpStatusCode.OK,
                message = mapOf(
                    "liveness" to "${probe.liveness}",
                    "readiness" to "${probe.readiness}"
                )
            )
        }
    }
}