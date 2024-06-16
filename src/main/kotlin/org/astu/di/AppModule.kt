package org.astu.di

import io.ktor.server.application.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module

fun Application.database(): Module {
    val dbString =
        environment.config.tryGetString("db.connectionString") ?: throw Exception("cannot init")
    val dbUser =
        environment.config.tryGetString("db.user") ?: throw Exception("cannot init")
    val dbPassword =
        environment.config.tryGetString("db.password") ?: throw Exception("cannot init")
    val dbDriver =
        environment.config.tryGetString("db.driver") ?: throw Exception("cannot init")

    return module {
        single {
            Database.connect(
                url = dbString,
                driver = dbDriver,
                user = dbUser,
                password = dbPassword
            )
        }
    }
}
