package org.astu.plugins

import io.ktor.server.application.*
import org.astu.di.database
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule
import org.koin.ktor.plugin.Koin

fun Application.di() {
    install(Koin) {
        val module = module {
            single { this@di }
        }
        this.modules(defaultModule, module, this@di.database())
    }
}