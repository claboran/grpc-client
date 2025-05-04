package de.laboranowitsch.poc.grpcclient.application

import de.laboranowitsch.poc.grpcclient.application.model.CalculationJob
import de.laboranowitsch.poc.grpcclient.application.model.ProcessedResult
import de.laboranowitsch.poc.grpcclient.application.ports.CalculationJobRepository
import de.laboranowitsch.poc.grpcclient.application.ports.ExternalComputationPort
import de.laboranowitsch.poc.grpcclient.common.LoggingAware
import de.laboranowitsch.poc.grpcclient.common.logger
import de.laboranowitsch.poc.grpcclient.interfaces.CalculationPort
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationRequest
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationResponse
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationStatus
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationStatus.Status
import de.laboranowitsch.poc.grpcclient.protobuf.InputParamItem
import de.laboranowitsch.poc.grpcclient.protobuf.OutputChunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.*

@Service
class CalculationService(
    private val externalComputationPort: ExternalComputationPort,
    private val jobRepository: CalculationJobRepository,
    private val applicationScope: CoroutineScope,
) : CalculationPort, LoggingAware {

    private val logger = logger()

    override fun initiateCalculation(inputs: List<String>): String {
        val jobId = UUID.randomUUID().toString()
        try {
            jobRepository.saveInitial(
                CalculationJob(
                    id = jobId,
                    inputCount = inputs.size,
                    status = Status.ACCEPTED.name,
                )
            )
            logger.info("Accepted calculation job: {}", jobId)
        } catch (e: Exception) {
            logger.error("[{}] Failed to save initial job state", jobId, e)
            throw RuntimeException("Failed to initialize job state", e)
        }

        applicationScope.launch {
            processJobInBackground(jobId, inputs)
        }
        return jobId
    }


    override fun getCalculationStatus(jobId: String): String? = jobRepository.findById(jobId)?.status

    override fun getCalculationResults(jobId: String): List<String>? = jobRepository
        .getResults(jobId)?.map { it.resultValue }


    private suspend fun processJobInBackground(jobId: String, inputs: List<String>) {
        logger.info("[{}] Starting background processing...", jobId)
        val request = prepareCalculationRequest(jobId, inputs)
        var receivedItemCount = 0L

        try {
            externalComputationPort.processCalculation(request)
                .catch { e -> handleStreamError(jobId, e) }
                .collect { response ->
                    when (response.responseTypeCase) {
                        CalculationResponse.ResponseTypeCase.STATUS_UPDATE -> {
                            handleStatusUpdate(jobId, response.statusUpdate)
                        }

                        CalculationResponse.ResponseTypeCase.OUTPUT_CHUNK -> {
                            receivedItemCount = processOutputChunk(jobId, response.outputChunk, receivedItemCount)
                        }

                        CalculationResponse.ResponseTypeCase.RESPONSETYPE_NOT_SET, null -> {
                            logger.warn("[{}] Received response with unknown or unset type.", jobId)
                        }
                    }
                }

            handleFlowCompletion(jobId, receivedItemCount)

        } catch (e: Exception) {
            handleProcessingException(jobId, e)
        }
    }

    internal fun prepareCalculationRequest(jobId: String, inputs: List<String>): CalculationRequest =
        CalculationRequest.newBuilder()
            .setJobId(jobId)
            .addAllInputItems(inputs.map { InputParamItem.newBuilder().setValue(it).build() })
            .build()

    internal fun handleStreamError(jobId: String, e: Throwable) {
        val errorMessage = "Error in gRPC stream for job $jobId: ${e.message}"
        logger.error(errorMessage, e)
        jobRepository.updateStatus(
            jobId,
            Status.FAILED.name,
            "gRPC stream error: ${e.message}",
        )
        throw e
    }

    internal fun handleStatusUpdate(jobId: String, statusUpdate: CalculationStatus) {
        logger.info("[{}] Status update received: {}", jobId, statusUpdate.status)
        jobRepository.updateStatus(
            jobId,
            statusUpdate.status.name,
            statusUpdate.message,
        )
    }

    internal fun processOutputChunk(
        jobId: String,
        outputChunk: OutputChunk,
        currentCount: Long
    ): Long {
        var receivedItemCount = currentCount
        logger.debug("[{}] Received output chunk with {} items.", jobId, outputChunk.itemsCount)
        outputChunk.itemsList.forEach { outputItem ->
            val processedResult = ProcessedResult(
                jobId = jobId,
                resultValue = outputItem.value
            )
            jobRepository.addResult(jobId, processedResult)
            receivedItemCount++
        }
        if (receivedItemCount % 100 == 0L) {
            logger.debug("[{}] Processed {} total result items.", jobId, receivedItemCount)
        }
        return receivedItemCount
    }

    internal fun handleFlowCompletion(jobId: String, receivedItemCount: Long) {
        logger.info(
            "[{}] Response stream processing completed. Processed {} total result items.",
            jobId,
            receivedItemCount,
        )
        val finalJob = jobRepository.findById(jobId)
        if (
            finalJob?.status != Status.FAILED.name
            && finalJob?.status != Status.FINISHED.name
        ) {
            logger.warn(
                "[{}] Stream ended but final status was {}. Setting to FINISHED (assumed success).",
                jobId,
                finalJob?.status,
            )
            jobRepository.updateStatus(
                jobId,
                Status.FINISHED.name,
                "Stream completed.",
            ) // Assume success if no failure/finish received
        }
    }

    internal fun handleProcessingException(jobId: String, e: Exception) {
        val errorMessage = "Error collecting/processing results for job $jobId: ${e.message}"
        logger.error(errorMessage, e)
        if (jobRepository.findById(jobId)?.status != Status.FAILED.name) {
            jobRepository.updateStatus(
                jobId,
                Status.FAILED.name,
                "Processing error: ${e.message}",
            )
        }
    }
}
