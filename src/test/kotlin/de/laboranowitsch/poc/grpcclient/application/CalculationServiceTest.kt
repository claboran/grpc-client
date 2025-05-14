package de.laboranowitsch.poc.grpcclient.application

import de.laboranowitsch.poc.grpcclient.application.model.CalculationJob
import de.laboranowitsch.poc.grpcclient.application.ports.CalculationJobRepository
import de.laboranowitsch.poc.grpcclient.application.ports.ExternalComputationPort
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationStatus
import de.laboranowitsch.poc.grpcclient.protobuf.OutputChunk
import de.laboranowitsch.poc.grpcclient.protobuf.OutputParamItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class CalculationServiceTest {

    @Mock
    private lateinit var jobRepository: CalculationJobRepository

    @Mock
    private lateinit var externalComputationPort: ExternalComputationPort

    private lateinit var calculationService: CalculationService

    @BeforeEach
    fun setup() {
        calculationService = CalculationService(
            externalComputationPort,
            jobRepository,
            CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `prepareCalculationRequest should create request with correct job ID and inputs`() {
        // Arrange
        val jobId = "test-job-1"
        val inputs = listOf("input1", "input2", "input3")

        // Act
        val request = calculationService.prepareCalculationRequest(jobId, inputs)

        // Assert
        assertEquals(jobId, request.jobId)
        assertEquals(3, request.inputItemsCount)
        assertEquals("input1", request.getInputItems(0).value)
        assertEquals("input2", request.getInputItems(1).value)
        assertEquals("input3", request.getInputItems(2).value)
    }

    @Test
    fun `handleStatusUpdate should update job status in repository`() {
        // Arrange
        val jobId = "test-job-2"
        val statusUpdate = CalculationStatus.newBuilder()
            .setStatus(CalculationStatus.Status.IN_PROGRESS)
            .setMessage("Processing job")
            .build()

        // Add this line to define mock behavior
        doNothing().whenever(jobRepository).updateStatus(any(), any(), any())

        // Act
        calculationService.handleStatusUpdate(jobId, statusUpdate)

        // Assert
        verify(jobRepository).updateStatus(
            jobId,
            "IN_PROGRESS",
            "Processing job"
        )
    }

    @Test
    fun `processOutputChunk should add results to repository and return updated count`() {
        // Arrange
        val jobId = "test-job-3"
        val currentCount = 5L

        val item1 = OutputParamItem.newBuilder().setValue("result1").build()
        val item2 = OutputParamItem.newBuilder().setValue("result2").build()

        val outputChunk = OutputChunk.newBuilder()
            .addItems(item1)
            .addItems(item2)
            .build()

        doNothing().whenever(jobRepository).addResult(any(), any())

        // Act
        val newCount = calculationService.processOutputChunk(jobId, outputChunk, currentCount)

        // Assert
        assertThat(newCount).isEqualTo(7L) // 5 + 2 new items
    }

    @Test
    fun `handleFlowCompletion should update status to FINISHED if not already FAILED or FINISHED`() {
        // Arrange
        val jobId = "test-job-4"
        val receivedItemCount = 10L

        // Job is still in PROCESSING state
        val job = CalculationJob(
            id = jobId,
            inputCount = 5,
            status = "PROCESSING"
        )
        whenever(jobRepository.findById(jobId)).thenReturn(job)
        doNothing().whenever(jobRepository).updateStatus(any(), any(), any())

        // Act
        calculationService.handleFlowCompletion(jobId, receivedItemCount)

        // Assert
        verify(jobRepository).updateStatus(
            jobId,
            "FINISHED",
            "Stream completed."
        )
    }

    @Test
    fun `handleFlowCompletion should not update status if already FAILED`() {
        // Arrange
        val jobId = "test-job-5"
        val receivedItemCount = 10L

        // Job is already in FAILED state
        val job = CalculationJob(
            id = jobId,
            inputCount = 5,
            status = "FAILED",
            statusMessage = "Previous error"
        )
        whenever(jobRepository.findById(jobId)).thenReturn(job)

        // Act
        calculationService.handleFlowCompletion(jobId, receivedItemCount)

        // Assert
        verify(jobRepository, never()).updateStatus(
            jobId,
            "FINISHED",
            "Stream completed."
        )
    }

    @Test
    fun `handleProcessingException should update status to FAILED if not already FAILED`() {
        // Arrange
        val jobId = "test-job-6"
        val exception = RuntimeException("Test error")

        // Job is in PROCESSING state
        val job = CalculationJob(
            id = jobId,
            inputCount = 5,
            status = "PROCESSING"
        )
        whenever(jobRepository.findById(jobId)).thenReturn(job)
        doNothing().whenever(jobRepository).updateStatus(any(), any(), any())

        // Act
        calculationService.handleProcessingException(jobId, exception)

        // Assert
        verify(jobRepository).updateStatus(
            jobId,
            "FAILED",
            "Processing error: Test error"
        )
    }

    @Test
    fun `handleProcessingException should not update status if already FAILED`() {
        // Arrange
        val jobId = "test-job-7"
        val exception = RuntimeException("Test error")

        // Job is already in FAILED state
        val job = CalculationJob(
            id = jobId,
            inputCount = 5,
            status = "FAILED",
            statusMessage = "Previous error"
        )
        whenever(jobRepository.findById(jobId)).thenReturn(job)

        // Act
        calculationService.handleProcessingException(jobId, exception)

        // Assert
        verify(jobRepository, never()).updateStatus(
            jobId,
            "FAILED",
            "Processing error: Test error"
        )
    }
}
