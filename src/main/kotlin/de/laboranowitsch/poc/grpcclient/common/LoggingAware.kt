package de.laboranowitsch.poc.grpcclient.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.java

/**
 * Marker interface for classes that need logging capabilities.
 * Classes implementing this interface can use the logger() extension function.
 */
interface LoggingAware

/**
 * Extension function that provides a logger for any class implementing LoggingAware.
 * Uses reified type parameter to get the correct logger for the implementing class.
 *
 * @return SLF4J Logger instance for the class
 */
inline fun <reified T : LoggingAware> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)
