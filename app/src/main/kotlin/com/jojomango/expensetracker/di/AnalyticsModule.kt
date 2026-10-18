package com.jojomango.expensetracker.di

import com.jojomango.expensetracker.ui.analytics.AnalyticsConsent
import com.jojomango.expensetracker.ui.analytics.AnalyticsSink
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import com.jojomango.expensetracker.ui.analytics.FirebaseAnalyticsSink
import com.jojomango.expensetracker.ui.analytics.PreferencesAnalyticsConsent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalyticsSink(impl: FirebaseAnalyticsSink): AnalyticsSink

    @Binds
    abstract fun bindAnalyticsConsent(impl: PreferencesAnalyticsConsent): AnalyticsConsent

    companion object {
        @Provides
        @Singleton
        fun provideAnalyticsTracker(
            consent: AnalyticsConsent,
            sink: AnalyticsSink,
        ): AnalyticsTracker = AnalyticsTracker(consent, sink)
    }
}
