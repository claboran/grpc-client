package de.laboranowitsch.poc.grpcclient.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import kotlin.coroutines.cancellation.CancellationException

@Configuration
class CoroutineConfig {

    private val applicationJob = SupervisorJob()
    private val applicationScope = CoroutineScope(Dispatchers.Default + applicationJob)

    @Bean
    fun applicationCoroutineScope(): CoroutineScope = applicationScope

    @EventListener(ContextClosedEvent::class)
    fun onContextClosed() = applicationJob.run {
        cancel(CancellationException("Application shutting down"))
    }

}
