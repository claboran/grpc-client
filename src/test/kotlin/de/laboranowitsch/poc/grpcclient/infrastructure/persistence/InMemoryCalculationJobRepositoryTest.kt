package de.laboranowitsch.poc.grpcclient.infrastructure.persistence

import de.laboranowitsch.poc.grpcclient.application.model.CalculationJob
import de.laboranowitsch.poc.grpcclient.application.model.ProcessedResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class InMemoryCalculationJobRepositoryTest {

    @Test
    fun `should save and retrieve a job`() {
        // Arrange
        val repository = InMemoryCalculationJobRepository()
        val job = CalculationJob(
            id = "test-job-1",
            inputCount = 5,
            status = "ACCEPTED"
        )

        // Act
        repository.saveInitial(job)
        val retrievedJob = repository.findById("test-job-1")

        // Assert
        assertNotNull(retrievedJob)
        assertEquals("test-job-1", retrievedJob?.id)
        assertEquals(5, retrievedJob?.inputCount)
        assertEquals("ACCEPTED", retrievedJob?.status)
        assertTrue(retrievedJob?.results?.isEmpty() ?: false)
    }

    @Test
    fun `should update job status`() {
        // Arrange
        val repository = InMemoryCalculationJobRepository()
        val job = CalculationJob(
            id = "test-job-2",
            inputCount = 3,
            status = "ACCEPTED"
        )
        repository.saveInitial(job)

        // Act
        repository.updateStatus("test-job-2", "PROCESSING", "Job is being processed")
        val retrievedJob = repository.findById("test-job-2")

        // Assert
        assertEquals("PROCESSING", retrievedJob?.status)
        assertEquals("Job is being processed", retrievedJob?.statusMessage)
    }

    @Test
    fun `should add and retrieve results`() {
        // Arrange
        val repository = InMemoryCalculationJobRepository()
        val job = CalculationJob(
            id = "test-job-3",
            inputCount = 2,
            status = "PROCESSING"
        )
        repository.saveInitial(job)

        // Act
        val result1 = ProcessedResult("test-job-3", "Result 1")
        val result2 = ProcessedResult("test-job-3", "Result 2")
        repository.addResult("test-job-3", result1)
        repository.addResult("test-job-3", result2)
        
        val results = repository.getResults("test-job-3")

        // Assert
        assertNotNull(results)
        assertEquals(2, results?.size)
        assertEquals("Result 1", results?.get(0)?.resultValue)
        assertEquals("Result 2", results?.get(1)?.resultValue)
    }
}