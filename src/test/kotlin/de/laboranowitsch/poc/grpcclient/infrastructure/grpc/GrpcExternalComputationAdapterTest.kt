package de.laboranowitsch.poc.grpcclient.infrastructure.grpc

import de.laboranowitsch.poc.grpcclient.protobuf.CalculationRequest
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationResponse
import de.laboranowitsch.poc.grpcclient.protobuf.ComputationServiceGrpcKt
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class GrpcExternalComputationAdapterTest {

    @Mock
    private lateinit var clientStub: ComputationServiceGrpcKt.ComputationServiceCoroutineStub

    private lateinit var adapter: GrpcExternalComputationAdapter

    @BeforeEach
    fun setup() {
        adapter = GrpcExternalComputationAdapter()
        // Use reflection to set the private field
        val field = GrpcExternalComputationAdapter::class.java.getDeclaredField("clientStub")
        field.isAccessible = true
        field.set(adapter, clientStub)
    }

    @Test
    fun `processCalculation should return flow from client stub`() {
        runBlocking {
            // Arrange
            val request = CalculationRequest.newBuilder()
                .setJobId("test-job-1")
                .build()

            val response = CalculationResponse.newBuilder().build()
            whenever(clientStub.processCalculation(any(), any())).thenReturn(flowOf(response))

            // Act
            val result = adapter.processCalculation(request)

            // Assert
            assertThat(result.toList()).hasSize(1)
            assertThat(result.toList()[0]).isEqualTo(response)
        }
    }

    @Test
    fun `processCalculation should handle StatusRuntimeException with INVALID_ARGUMENT`() {
        runBlocking {
            // Arrange
            val request = CalculationRequest.newBuilder()
                .setJobId("test-job-2")
                .build()

            val exception = StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("Invalid input")
            )
            whenever(clientStub.processCalculation(any(), any())).thenThrow(exception)

            // Act & Assert
            val thrownException = assertThrows<RuntimeException> {
                adapter.processCalculation(request)
            }

            assertThat(thrownException).hasMessageContaining("gRPC communication failed immediately")
            assertThat(thrownException.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `processCalculation should handle StatusRuntimeException with UNAVAILABLE`() {
        runBlocking {
            // Arrange
            val request = CalculationRequest.newBuilder()
                .setJobId("test-job-3")
                .build()

            val exception = StatusRuntimeException(
                Status.UNAVAILABLE.withDescription("Service unavailable")
            )
            whenever(clientStub.processCalculation(any(), any())).thenThrow(exception)

            // Act & Assert
            val thrownException = assertThrows<RuntimeException> {
                adapter.processCalculation(request)
            }

            assertThat(thrownException).hasMessageContaining("gRPC communication failed immediately")
            assertThat(thrownException.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `processCalculation should handle generic exception`() {
        runBlocking {
            // Arrange
            val request = CalculationRequest.newBuilder()
                .setJobId("test-job-4")
                .build()

            val exception = RuntimeException("Generic error")
            whenever(clientStub.processCalculation(any(), any())).thenThrow(exception)

            // Act & Assert
            val thrownException = assertThrows<RuntimeException> {
                adapter.processCalculation(request)
            }

            assertThat(thrownException).hasMessageContaining("Unexpected gRPC client error")
            assertThat(thrownException.cause).isEqualTo(exception)
        }
    }
}
