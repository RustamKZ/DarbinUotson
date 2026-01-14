package org.example.project_dw.shared.datasources.python

import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer

object PythonSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun <T> serialize(serializer: KSerializer<T>, value: T): String {
        return json.encodeToString(serializer, value)
    }

    fun <T> deserialize(serializer: KSerializer<T>, jsonString: String): T {
        return json.decodeFromString(serializer, jsonString)
    }
}
