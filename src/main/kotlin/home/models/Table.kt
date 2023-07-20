package home.models

import org.jetbrains.exposed.dao.id.IntIdTable

object UserTable : IntIdTable() {
    val userName = varchar("username", LENGTH_DEFAULT)
    val firstName = varchar("firstName", LENGTH_DEFAULT)
    val lastName = varchar("lastName", LENGTH_DEFAULT)
    val email = varchar("email", LENGTH_DEFAULT)
    val phone = varchar("phone", LENGTH_DEFAULT)
}

private const val LENGTH_DEFAULT = 256