package home.models

import home.plugins.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

interface Dao {
    suspend fun addUser(user: UserEntity): Int?
    suspend fun getUser(id: String): UserEntity?
    suspend fun deleteUser(id: String): Boolean
    suspend fun updateUser(id: String, payload: UserEntity): Int?
    suspend fun getAll(): List<Int>?
    suspend fun deleteAll()
}

class DaoImpl : Dao {
    private fun resultRowToUserEntity(row: ResultRow): UserEntity = UserEntity(
        userName = row[UserTable.userName],
        firstName = row[UserTable.firstName],
        lastName = row[UserTable.lastName],
        email = row[UserTable.email],
        phone = row[UserTable.phone]
    )

    private fun resultRowToUserId(row: ResultRow): Int = row[UserTable.id].value

    override suspend fun addUser(user: UserEntity): Int? = dbQuery {
        UserTable.insertIgnoreAndGetId {
            it[id] = user.id
            it[userName] = user.userName
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[email] = user.email
            it[phone] = user.phone
        }?.value
    }

    override suspend fun getUser(id: String): UserEntity? = dbQuery {
        UserTable
            .select { UserTable.id eq id.toIntOrNull() }
            .map(::resultRowToUserEntity)
            .singleOrNull()
    }

    override suspend fun deleteUser(id: String): Boolean = dbQuery {
        UserTable.deleteWhere { UserTable.id eq id.toIntOrNull() } > 0
    }

    override suspend fun deleteAll() = dbQuery {
        UserTable.deleteAll(); Unit
    }

    override suspend fun updateUser(id: String, payload: UserEntity): Int? = dbQuery {
        (UserTable.update({ UserTable.id eq id.toIntOrNull() }) {
            it[UserTable.id] = payload.id
            it[userName] = payload.userName
            it[firstName] = payload.firstName
            it[lastName] = payload.lastName
            it[email] = payload.email
            it[phone] = payload.phone
        } > 0).let { isSuccess ->
            if (isSuccess) payload.id else null
        }
    }

    override suspend fun getAll(): List<Int>? = dbQuery {
        UserTable
            .selectAll()
            .map(::resultRowToUserId)
            .takeUnless { it.isEmpty() }
    }
}