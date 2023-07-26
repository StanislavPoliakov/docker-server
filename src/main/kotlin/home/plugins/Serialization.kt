package home.plugins

import home.models.Dao
import home.models.UserEntity
import home.models.id
import home.probes.Probe
import home.probes.ProbeStatus
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.delay
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

fun Application.configureSerialization(probe: Probe, database: Dao, registry: PrometheusMeterRegistry) {
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
                        status = HttpStatusCode.NotFound,
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
                    message = mapOf(
                        "message" to "User created!",
                        "ID" to Base64.getEncoder()
                            .withoutPadding()
                            .encodeToString("$id".toByteArray())
                    )
                )
            } ?: call.respond(
                status = HttpStatusCode.OK,
                message = mapOf(
                    "message" to "This User is already exists!",
                    "ID" to Base64.getEncoder()
                        .withoutPadding()
                        .encodeToString("${user.id}".toByteArray())
                )
            )
        }

        delete("/user") {
            call.request.queryParameters["id"]?.let { id ->
                val stringId = String(Base64.getDecoder().decode(id))
                if (database.deleteUser(stringId)) {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = mapOf(
                            "message" to "User successfully deleted!",
                            "ID" to id
                        )
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
                        message = mapOf(
                            "message" to "User successfully updated!",
                            "ID" to Base64.getEncoder()
                                .withoutPadding()
                                .encodeToString("$newId".toByteArray())
                        )
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
        get("/metrics") {
            call.respond(registry.scrape())
        }

        get("/nt-get-fast") {
            if (doFastWork()) {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "Job is done (fast)!"
                )
            } else {
                call.respond(
                    status = HttpStatusCode.ServiceUnavailable,
                    message = "Service unavailable!"
                )
            }
        }

        get("/nt-get-slow") {
            if (doSlowWork()) {
                call.respond(
                    status = HttpStatusCode.OK,
                    message = "Job is done (slow)!"
                )
            } else {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = "Content not found!"
                )
            }
        }

        post("/nt-post-populate-db") {
            populateDb(database)
            call.respond(
                status = HttpStatusCode.OK,
                message = "User added!"
            )
        }

        delete("/nt-delete-all") {
            database.deleteAll()
            call.respond(
                status = HttpStatusCode.OK,
                message = "Database cleared!"
            )
        }
    }
}

private val randomFast = Random(3L)
private val randomSlow = Random(9L)
private val randomDb = Random(Long.MAX_VALUE)

private suspend fun doFastWork(): Boolean {
    val delayValue = randomFast.nextLong(0, 3L)
    delay((delayValue* 100).milliseconds)
    return delayValue % 3 != 0L
}

private suspend fun doSlowWork(): Boolean {
    val delayValue = randomSlow.nextLong(0L, 9L)
    delay((delayValue* 100).milliseconds)
    return delayValue % 3 != 0L
}

private suspend fun populateDb(database: Dao) {
    val salt = randomDb.nextLong()
    database.addUser(
        UserEntity(
            userName = "NT_username_$salt",
            firstName = "NT_firstName",
            lastName = "NT_lastName",
            email = "NT_email",
            phone = "NT_phone"
        )
    )
}