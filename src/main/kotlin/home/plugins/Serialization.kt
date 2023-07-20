package home.plugins

import home.models.Dao
import home.models.UserEntity
import home.probes.Probe
import home.probes.ProbeStatus
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.configureSerialization(probe: Probe, database: Dao) {
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

        get("/user") {
            call.request.queryParameters["id"]?.let { id ->
                val stringId = String(Base64.getDecoder().decode(id))
                database.getUser(stringId)?.let { user ->
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = user
                    )
                } ?: run {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = "No User found!"
                    )
                }
            } ?: call.respond(
                status = HttpStatusCode.BadRequest,
                message = "No \"id\" specified!"
            )
        }

        post("/user") {
            val user = call.receive<UserEntity>()
            database.addUser(user)?.also { id ->
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "User created! ID = ${
                        Base64.getEncoder()
                            .withoutPadding()
                            .encodeToString("$id".toByteArray())
                    }"
                )
            } ?: call.respond(
                status = HttpStatusCode.OK,
                message = "This User is already exists!"
            )
        }

        delete("/user") {
            call.request.queryParameters["id"]?.let { id ->
                val stringId = String(Base64.getDecoder().decode(id))
                if (database.deleteUser(stringId)) {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = "User [$id] successfully deleted!"
                    )
                } else {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = "No User found!"
                    )
                }
            } ?: call.respond(
                status = HttpStatusCode.BadRequest,
                message = "No \"id\" specified!"
            )
        }

        put("/user") {
            call.request.queryParameters["id"]?.let { id ->
                val payload = call.receive<UserEntity>()
                val decodedId = String(Base64.getDecoder().decode(id))
                database.updateUser(decodedId, payload)?.let { newId ->
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = buildString {
                            append("User successfully updated! ")
                            if (decodedId.toIntOrNull() != newId) append(
                                "New ID = ${
                                    Base64.getEncoder()
                                        .withoutPadding()
                                        .encodeToString("$newId".toByteArray())
                                }"
                            )
                        }
                    )
                } ?: run {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = "Not updated!"
                    )
                }
            } ?: call.respond(
                status = HttpStatusCode.BadRequest,
                message = "No \"id\" specified!"
            )
        }

        get("/users") {
            database.getAll()
                ?.map { id ->
                    Base64.getEncoder()
                        .withoutPadding()
                        .encodeToString("$id".toByteArray())
                }?.let {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = it.joinToString(separator = "\n")
                    )
                } ?: run {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "No Users found!"
                )
            }
        }
    }
}