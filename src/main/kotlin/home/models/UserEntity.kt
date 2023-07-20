package home.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserEntity(
    val userName: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String
)

val UserEntity.id: Int
    get() = Objects.hash(userName, firstName, lastName)
