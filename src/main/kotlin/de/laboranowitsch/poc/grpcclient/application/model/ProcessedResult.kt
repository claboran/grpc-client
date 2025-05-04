package de.laboranowitsch.poc.grpcclient.application.model

import java.time.Instant

data class ProcessedResult(
    val jobId: String,
    val resultValue: String,
    val processedTimestamp: Instant = Instant.now()
)

