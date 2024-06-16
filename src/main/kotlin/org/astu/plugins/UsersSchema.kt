package org.astu.plugins

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.astu.models.Template
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Factory
import java.util.*

@Factory
class TemplateRepository(val database: Database) {
    object Templates : Table() {
        val id = uuid("id")
        val name = text("name")
        val description = text("description")
        val filepath = text("filepath")
        val filename = text("filename")
        val checksum = binary("sum", 120)
        val field = text("fields")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Templates)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(template: Template) = dbQuery {
        Templates.insert {
            it[id] = template.id
            it[name] = template.name
            it[description] = template.description
            it[filepath] = template.filePath
            it[filename] = template.fileName
            it[checksum] = template.checksum
            it[field] = Json.encodeToString(template.fields)
        }
    }

    suspend fun readAll(): List<Template> = dbQuery {
        Templates.selectAll().map(::assignTemplates)
    }

    suspend fun read(id: UUID): Template? = dbQuery {
        Templates.select(Templates.id eq id).map(::assignTemplates).singleOrNull()
    }

    suspend fun findBySumAndName(sum: ByteArray, name: String): Template? = dbQuery {
        Templates.select((Templates.checksum eq sum) and (Templates.filename eq name))
            .map(::assignTemplates)
            .singleOrNull()
    }

    private fun assignTemplates(resultRow: ResultRow): Template {
        val template = Template(
            resultRow[Templates.id],
            resultRow[Templates.name],
            resultRow[Templates.description],
            resultRow[Templates.filepath],
            resultRow[Templates.filename],
            resultRow[Templates.checksum],
            Json.decodeFromString(resultRow[Templates.field])
        )
        return template
    }
}
