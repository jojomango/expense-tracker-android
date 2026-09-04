package com.jojomango.expensetracker.di

import com.jojomango.expensetracker.data.DataMarker
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Placeholder proving `di` can wire `data` into the Hilt graph. Real
 * Repository bindings land starting Phase 3.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DiMarker {
    val referencesDataMarker: DataMarker = DataMarker
}
