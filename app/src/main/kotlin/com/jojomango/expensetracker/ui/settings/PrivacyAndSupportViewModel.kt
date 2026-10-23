package com.jojomango.expensetracker.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.jojomango.expensetracker.ui.analytics.AnalyticsEvent
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import com.jojomango.expensetracker.ui.donation.DonationMessage
import com.jojomango.expensetracker.ui.donation.DonationOffers
import com.jojomango.expensetracker.ui.donation.DonationTier
import com.jojomango.expensetracker.ui.donation.PlayDonationBilling
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** 設定頁的「隱私」與「支持開發者」兩個區塊（UI-SPEC.md §10）。 */
@HiltViewModel
class PrivacyAndSupportViewModel
    @Inject
    constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val donationBilling: PlayDonationBilling,
    ) : ViewModel() {
        private val analyticsEnabledState = MutableStateFlow(analyticsTracker.isEnabled)
        val analyticsEnabled: StateFlow<Boolean> = analyticsEnabledState.asStateFlow()

        val donationOffers: StateFlow<DonationOffers> = donationBilling.offers

        /** 付款結果的訊息（謝謝／無法連線），跟備份訊息共用設定頁的 Snackbar。 */
        val donationMessages: SharedFlow<DonationMessage> = donationBilling.messages

        fun setAnalyticsEnabled(enabled: Boolean) {
            analyticsTracker.setEnabled(enabled)
            analyticsEnabledState.value = enabled
        }

        /** 使用者點開「支持開發者」：這時才連 Play 查價格（SPEC.md P3——沒點開就不為了贊助連網）。 */
        fun onDonationSectionOpened() {
            analyticsTracker.track(AnalyticsEvent.DonationSectionOpened)
            donationBilling.refresh()
        }

        /** Activity 只用來開 Play 的付款畫面，不會被存起來。 */
        fun donate(
            activity: Activity,
            tier: DonationTier,
        ) {
            donationBilling.launch(activity, tier)
        }
    }
