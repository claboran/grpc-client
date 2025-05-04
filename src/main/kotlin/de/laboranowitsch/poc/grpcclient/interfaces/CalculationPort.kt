package de.laboranowitsch.poc.grpcclient.interfaces

interface CalculationPort {
    fun initiateCalculation(inputs: List<String>): String
    fun getCalculationStatus(jobId: String): String?
    fun getCalculationResults(jobId: String): List<String>?
}
