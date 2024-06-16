package org.astu.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.astu.dto.TemplateDTO
import org.astu.dto.TemplateFieldDTO
import org.astu.exceptions.*
import org.astu.models.*
import org.astu.plugins.TemplateRepository
import org.koin.core.annotation.Factory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.createParentDirectories

@Factory
class TemplateService(
    val documentService: DocDocumentService,
    val templateRepository: TemplateRepository
) {
    private val parentDir = "templates"

    suspend fun addTemplate(info: TemplateInfo, templateFile: TemplateFile) {
        val id = UUID.randomUUID()
        val path = "$parentDir/${id}/${templateFile.fileName}"
        val file = File(path)
        if (!isSupportedFormat(file))
            throw UnsupportedFileFormatException("${file.extension} не поддерживается")

        val checkSum = MessageDigest.getInstance("MD5").digest(templateFile.content)
//        templateRepository.findBySumAndName(checkSum, templateFile.fileName)?.run { throw TemplateExistException() }
        val inputStream = ByteArrayInputStream(templateFile.content)
        val listFields = documentService.readFields(inputStream)
        if (listFields.isEmpty())
            throw NotFoundFieldsException()

        createFile(file, templateFile.content)

        val template = Template(
            id,
            info.name,
            info.description,
            path,
            templateFile.fileName,
            checkSum,
            listFields.map { TemplateField(it) })
        templateRepository.create(template)
    }

    suspend fun loadTemplates(): List<TemplateDTO> {
        return templateRepository.readAll().map { template ->
            TemplateDTO(
                template.id.toString(),
                template.name,
                template.description,
                template.fields.map { it.name }
            )
        }
    }

    suspend fun fillTemplate(templateId: UUID, fields: List<TemplateFieldDTO>): File {
        val template = templateRepository.read(templateId) ?: throw TemplateNotExistException()

        val templateFile = File(template.filePath)

        if (!templateFile.exists())
            throw TemplateNotExistException()

        val allFieldIsFilled = template.fields.all { templateFields -> fields.any { templateFields.name == it.name } }

        if(!allFieldIsFilled)
            throw NotAllFieldsFilledException()

        val path = withContext(Dispatchers.IO) {
            Files.createTempFile("temp", "${templateId.version()}")
        }
        val file = path.toFile()
        file.deleteOnExit()

        file.outputStream().use {
            documentService.replaceFields(templateFile, fields, it)
        }
        return file
    }

    private fun createFile(file: File, bytes: ByteArray): Boolean {
        if (!file.parentFile.exists()) {
            runCatching {
                file.toPath().createParentDirectories()
            }.onFailure {
                return false
            }
        }
        file.writeBytes(bytes)
        return true
    }

    private fun isSupportedFormat(file: File): Boolean {
        val format = file.extension.lowercase()
        return SupportedTypes.entries.any { it.format == format }
    }
}