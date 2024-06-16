package org.astu.models

import java.util.*

class Template(
    val id: UUID,
    val name: String,
    val description: String,
    val filePath: String,
    val fileName: String,
    val checksum: ByteArray,
    val fields: List<TemplateField> = mutableListOf()
)

