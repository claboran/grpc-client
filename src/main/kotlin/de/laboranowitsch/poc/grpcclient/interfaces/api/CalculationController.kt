package de.laboranowitsch.poc.grpcclient.interfaces.api

import de.laboranowitsch.poc.grpcclient.common.LoggingAware
import de.laboranowitsch.poc.grpcclient.common.logger
import de.laboranowitsch.poc.grpcclient.interfaces.CalculationPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class StartRequest(val inputs: List<String>)
data class StartResponse(val jobId: String)
data class StatusResponse(val status: String?, val message: String? = null)
data class ResultsResponse(val results: List<String>?)

@RestController
@RequestMapping("/api/calculate")
class CalculationController(
    private val calculationPort: CalculationPort,
) : LoggingAware {

    private val logger = logger()

    @PostMapping("/start")
    fun startCalculation(@RequestBody request: StartRequest): ResponseEntity<StartResponse> = try {
        if (request.inputs.isEmpty()) {
            ResponseEntity.badRequest().body(StartResponse(jobId = "ERROR: Input list cannot be empty"))
        }
        if (request.inputs.size > 1000) { // Match proto limit
            ResponseEntity.badRequest()
                .body(StartResponse(jobId = "ERROR: Input list exceeds limit of 1000"))
        }

        ResponseEntity.status(HttpStatus.ACCEPTED).body(
            StartResponse(
                calculationPort.initiateCalculation(request.inputs)
                    .also { logger.info("Calculation job {} initiated via REST", it) },
            )
        )
    } catch (e: Exception) {
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StartResponse(jobId = "ERROR: ${e.message}"))
            .also { logger.error("Error initiating calculation request", e) }
    }


    @GetMapping("/{jobId}/status")
    fun getStatus(@PathVariable jobId: String): ResponseEntity<StatusResponse> =
        calculationPort.getCalculationStatus(jobId)?.let { status ->
            ResponseEntity.ok(StatusResponse(status = status))
        } ?: ResponseEntity.notFound().build()

    @GetMapping("/{jobId}/results")
    fun getResults(@PathVariable jobId: String): ResponseEntity<ResultsResponse> =
        calculationPort.getCalculationResults(jobId)?.let { results ->
            ResponseEntity.ok(ResultsResponse(results))
        } ?: run {
            calculationPort.getCalculationStatus(jobId)?.let { status ->
                ResponseEntity.accepted()
                    .body(ResultsResponse(results = listOf("Job status: $status. Results not yet available.")))
            } ?: ResponseEntity.notFound().build()
        }
}
