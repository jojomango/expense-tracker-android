package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.DomainMarker

/**
 * Placeholder proving `data` can depend on `domain`. Room entities/DAOs and
 * repository implementations land starting Phase 3.
 */
internal object DataMarker {
    val referencesDomainPhase: Int = DomainMarker.PHASE
}
