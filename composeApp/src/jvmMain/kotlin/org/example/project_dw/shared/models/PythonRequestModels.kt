package org.example.project_dw.shared.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class TimeSeriesRequest(
  val series: List<SeriesData>,
  @SerialName("target_index")
  val targetIndex: Int? = null
)

@Serializable
data class SeriesData(
  val name: String,
  val data: List<Double>
)