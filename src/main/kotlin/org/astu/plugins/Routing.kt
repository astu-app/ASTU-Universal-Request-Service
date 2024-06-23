package org.astu.plugins

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*
import org.astu.endpoints.templateEndpoints
import org.astu.exceptions.NotAllFieldsFilledException
import org.astu.exceptions.NotFoundFieldsException
import org.astu.exceptions.TemplateNotExistException
import org.astu.exceptions.UnsupportedFileFormatException

fun Application.configureRouting() {
    install(Webjars) {
        path = "/webjars" //defaults to /webjars
    }
    install(SwaggerUI) {
        swagger {
            swaggerUrl = "swagger"
            forwardRoot = true
        }
        info {
            title = "Universal Request API"
            version = "latest"
            description = "Апи для работы с шаблонами документов и получением сгенерированного документа"
        }
        server {
            url = "http://localhost:8080"
            description = "Development Server"
        }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        exception<NotAllFieldsFilledException> { call, cause ->
            call.respondText(text = "Не все поля заполнены", status = HttpStatusCode.BadRequest)
        }
        exception<NotFoundFieldsException> { call, cause ->
            call.respondText(text = "Документ не содержит поля или не удалось их найти", status = HttpStatusCode.BadRequest)
        }
        exception<UnsupportedFileFormatException> { call, cause ->
            call.respondText(text = "${cause.message}", status = HttpStatusCode.BadRequest)
        }
        exception<TemplateNotExistException> { call, cause ->
            call.respondText(text = "Не удалось найти шаблон заявления", status = HttpStatusCode.BadRequest)
        }
    }
    routing {
        route("api")  {
            templateEndpoints()
        }
        get("ping"){
            call.respondText("pong")
        }
    }
}
