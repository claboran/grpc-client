package de.laboranowitsch.poc.grpcclient.application.model

import java.time.Instant

data class CalculationJob(
    val id: String,
    var status: String = "UNKNOWN",
    var statusMessage: String? = null,
    val inputCount: Int,
    val createdAt: Instant = Instant.now(),
    var results: MutableList<ProcessedResult> = mutableListOf()
)
