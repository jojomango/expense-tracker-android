package com.jojomango.expensetracker.ui

import com.jojomango.expensetracker.domain.DomainMarker

/**
 * Placeholder proving `ui` can depend on `domain`. Real screens land
 * starting Phase 4.
 */
internal object UiMarker {
    val referencesDomainPhase: Int = DomainMarker.PHASE
}
