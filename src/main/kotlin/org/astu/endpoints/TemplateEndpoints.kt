package org.astu.endpoints

import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.astu.dto.TemplateFieldDTO
import org.astu.models.TemplateFile
import org.astu.models.TemplateInfo
import org.astu.service.TemplateService
import org.koin.ktor.ext.inject
import java.io.File
import java.util.*

fun Route.templateEndpoints(){
    post("templates/upload", {
        this.summary = "Загрузка шаблонов"
        request {
            multipartBody {
                part<File>("file") {
                    ContentType.MultiPart.FormData
                }
                part<TemplateInfo>("info") {
                    ContentType.Application.Json
                }
            }
        }
    })
    {
        val service by call.inject<TemplateService>()

        val files = call.receiveMultipart()
        var templateFile: TemplateFile? = null
        var templateInfo: TemplateInfo? = null
        files.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    templateInfo = Json.decodeFromString<TemplateInfo>(part.value)
                }

                is PartData.FileItem -> {
                    val filename = part.originalFileName ?: return@forEachPart
                    val bytes = part.streamProvider().readBytes()

                    templateFile = TemplateFile(filename, bytes)
                }

                else -> {
                    call.respond(HttpStatusCode.UnsupportedMediaType)
                }
            }
        }

        if (templateFile != null && templateInfo != null)
            service.addTemplate(templateInfo!!, templateFile!!)
        else
            call.respond(HttpStatusCode.BadRequest)

        call.respond(HttpStatusCode.OK)
    }

    get("templates", {
        summary = "Получение списка шаблонов"
    }) {
        val service by call.inject<TemplateService>()
        val templates = service.loadTemplates()
        call.respond(templates)
    }

    post("templates/{id}", {
        summary = "Заполнение шаблона"
        request {
            this.pathParameter<UUID>("id")
            body<List<TemplateFieldDTO>>()
        }
    }) {
        val id = call.parameters["id"] ?: return@post
        val list = call.receive<List<TemplateFieldDTO>>()
        val service by call.inject<TemplateService>()
        val template = service.fillTemplate(UUID.fromString(id), list)
        call.respondFile(template)
    }
}