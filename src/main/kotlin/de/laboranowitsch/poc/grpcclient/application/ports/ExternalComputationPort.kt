package de.laboranowitsch.poc.grpcclient.application.ports

import de.laboranowitsch.poc.grpcclient.protobuf.CalculationRequest
import de.laboranowitsch.poc.grpcclient.protobuf.CalculationResponse
import kotlinx.coroutines.flow.Flow


interface ExternalComputationPort {
    fun processCalculation(request: CalculationRequest): Flow<CalculationResponse>
}
