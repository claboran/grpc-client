package de.laboranowitsch.poc.grpcclient.application.ports

import de.laboranowitsch.poc.grpcclient.application.model.CalculationJob
import de.laboranowitsch.poc.grpcclient.application.model.ProcessedResult

interface CalculationJobRepository {
    fun saveInitial(job: CalculationJob): CalculationJob
    fun findById(jobId: String): CalculationJob?
    fun updateStatus(jobId: String, status: String, message: String? = null)
    fun addResult(jobId: String, result: ProcessedResult)
    fun getResults(jobId: String): List<ProcessedResult>?
}
