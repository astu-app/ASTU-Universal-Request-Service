package org.astu.dto

import kotlinx.serialization.Serializable

@Serializable
class TemplateDTO(val id: String, val name: String, val description: String, val fields: List<String>)