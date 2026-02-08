package org.example.project_dw.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
  val error: String,
  val message: String
)