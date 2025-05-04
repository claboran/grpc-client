package de.laboranowitsch.poc.grpcclient.infrastructure.grpc

import de.laboranowitsch.poc.grpcclient.application.ports.ExternalComputationPort
import de.laboranowitsch.poc.grpcclient.common.LoggingAware
import de.laboranowitsch.poc.grpcclient.common.logger
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationRequest
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationResponse
import de.laboranowitsch.poc.grpcclient.protobuf.ComputationServiceGrpcKt
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component

@Component
class GrpcExternalComputationAdapter : ExternalComputationPort, LoggingAware {
    private val logger = logger()

    @GrpcClient("computation-service")
    private lateinit var clientStub: ComputationServiceGrpcKt.ComputationServiceCoroutineStub

    override fun processCalculation(request: CalculationRequest): Flow<CalculationResponse> = try {
        logger.info("[{}] Sending ProcessCalculation request to gRPC server", request.jobId)
        clientStub.processCalculation(request)
            .catch { e -> // Catch gRPC specific errors here
                handleException(e, request.jobId)
                throw when (e) {
                    is StatusRuntimeException -> RuntimeException("gRPC communication failed", e)
                    else -> RuntimeException("gRPC call failed", e)
                }
            }
    } catch (e: Exception) {
        handleException(e, request.jobId)
        throw when (e) {
            is StatusRuntimeException -> RuntimeException("gRPC communication failed immediately", e)
            else -> RuntimeException("Unexpected gRPC client error", e)
        }
    }

    private fun handleException(e: Throwable, jobId: String) = when (e) {
        is StatusRuntimeException -> {
            when (e.status.code) {
                io.grpc.Status.Code.INVALID_ARGUMENT ->
                    logger.error("[{}] Invalid argument: {}", jobId, e.status.description)

                io.grpc.Status.Code.UNAVAILABLE ->
                    logger.error("[{}] Service unavailable: {}", jobId, e.status.description)

                else ->
                    logger.error("[{}] gRPC error: {}", jobId, e.status.description)
            }
        }

        else -> logger.error("[{}] Unexpected error calling gRPC: {}", jobId, e.message, e)
    }
}
