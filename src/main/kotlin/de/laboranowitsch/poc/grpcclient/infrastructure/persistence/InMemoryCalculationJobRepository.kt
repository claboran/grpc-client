package de.laboranowitsch.poc.grpcclient.infrastructure.persistence

import de.laboranowitsch.poc.grpcclient.application.model.CalculationJob
import de.laboranowitsch.poc.grpcclient.application.model.ProcessedResult
import de.laboranowitsch.poc.grpcclient.application.ports.CalculationJobRepository
import de.laboranowitsch.poc.grpcclient.common.LoggingAware
import de.laboranowitsch.poc.grpcclient.common.logger
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryCalculationJobRepository : CalculationJobRepository, LoggingAware {

    private val logger = logger()
    private val jobs = ConcurrentHashMap<String, CalculationJob>()

    override fun saveInitial(job: CalculationJob): CalculationJob = job.also {
        logger.info("Saving initial job: {}", it.id)
        jobs[it.id] = it.copy(results = mutableListOf())
    }

    override fun findById(jobId: String): CalculationJob? = jobs[jobId]?.copy()

    override fun updateStatus(
        jobId: String, status: String,
        message: String?,
    ) {
        jobs.computeIfPresent(jobId) { _, existingJob ->
            logger.info("Updating status for job {}: {} -> {}", jobId, existingJob.status, status)
            existingJob.copy(status = status, statusMessage = message)
        } ?: logger.warn("Job {} not found for status update.", jobId)
    }

    override fun addResult(
        jobId: String,
        result: ProcessedResult,
    ) {
        jobs.computeIfPresent(jobId) { _, existingJob ->
            logger.debug("Adding result for job {}: {}", jobId, result.resultValue)
            val newResults = existingJob.results.toMutableList().apply { add(result) }
            existingJob.copy(results = newResults)
        } ?: logger.warn("Job {} not found for adding result.", jobId)
    }

    override fun getResults(jobId: String): List<ProcessedResult>? = jobs[jobId]?.results?.toList()
}
