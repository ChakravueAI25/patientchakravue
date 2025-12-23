package com.org.patientchakravue.dose

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A simple, global event bus for signaling that dose information should be refreshed.
 * This is used to decouple notification handling from the UI.
 */
object DoseRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /**
     * Emits a refresh event. This should be called by platform-specific notification handlers.
     */
    fun emit() {
        _events.tryEmit(Unit)
    }
}
