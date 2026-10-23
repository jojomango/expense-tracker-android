package com.jojomango.expensetracker.ui.donation

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.jojomango.expensetracker.ui.analytics.AnalyticsEvent
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play Billing 的薄轉接層：把 SDK 的回呼轉成 [DonationPurchase]／[DonationBillingResult]，
 * 判斷交給 [DonationPurchaseHandler]（T9.2 在純 JVM 測過）。
 *
 * 只在使用者展開「支持開發者」時才連線（[refresh]）——沒點開的使用者不會因為贊助功能而連網。
 * `@Singleton`：Play 的購買回呼可能在離開設定頁之後才到，要有一個活得比畫面久的接收者。
 */
@Singleton
class PlayDonationBilling
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val analyticsTracker: AnalyticsTracker,
    ) {
        private val handler = DonationPurchaseHandler()
        private val productDetails = mutableMapOf<DonationTier, ProductDetails>()

        private val offersState = MutableStateFlow<DonationOffers>(DonationOffers.Loading)
        val offers: StateFlow<DonationOffers> = offersState.asStateFlow()

        private val messageFlow = MutableSharedFlow<DonationMessage>(extraBufferCapacity = 4)
        val messages: SharedFlow<DonationMessage> = messageFlow

        private val client: BillingClient =
            BillingClient
                .newBuilder(context)
                .setListener { result, purchases ->
                    val actions =
                        synchronized(handler) {
                            handler.onPurchasesUpdated(classify(result), purchases.orEmpty().map(::toDonationPurchase))
                        }
                    perform(actions, fromPurchaseFlow = true)
                }
                // 消耗型商品必須開啟 one-time 的待處理購買，Play 才接受（例如超商代碼繳費）。
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build()

        fun refresh() {
            if (offersState.value !is DonationOffers.Available) offersState.value = DonationOffers.Loading
            if (client.isReady) {
                queryProducts()
                consumeLeftovers()
                return
            }
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            queryProducts()
                            consumeLeftovers()
                        } else {
                            offersState.value = DonationOffers.Unavailable
                        }
                    }

                    // enableAutoServiceReconnection 會在下一次呼叫時自己重連，這裡不用做事。
                    override fun onBillingServiceDisconnected() = Unit
                },
            )
        }

        fun launch(
            activity: Activity,
            tier: DonationTier,
        ) {
            val details = synchronized(productDetails) { productDetails[tier] }
            if (details == null) {
                messageFlow.tryEmit(DonationMessage.UNAVAILABLE)
                return
            }
            val params =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams
                                .newBuilder()
                                .setProductDetails(details)
                                .build(),
                        ),
                    ).build()
            val result = client.launchBillingFlow(activity, params)
            // 成功開啟付款畫面時，結果之後由 setListener 收到；這裡只處理「根本沒開起來」的情況。
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) consumeLeftovers()
                val actions = synchronized(handler) { handler.onPurchasesUpdated(classify(result), emptyList()) }
                perform(actions, fromPurchaseFlow = true)
            }
        }

        private fun queryProducts() {
            val products =
                DonationTier.entries.map { tier ->
                    QueryProductDetailsParams.Product
                        .newBuilder()
                        .setProductId(tier.productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, details ->
                val byTier =
                    details.productDetailsList.mapNotNull { product ->
                        DonationTier.fromProductId(product.productId)?.let { it to product }
                    }
                synchronized(productDetails) {
                    productDetails.clear()
                    productDetails.putAll(byTier)
                }
                val prices =
                    byTier
                        .mapNotNull { (tier, product) -> product.oneTimePurchaseOfferDetails?.formattedPrice?.let { tier to it } }
                        .toMap()
                offersState.value = donationOffers(classify(result), prices)
            }
        }

        /** 上次付款後 App 在 consume 之前被關掉的話，那筆購買還卡著，同一個商品就不能再買（T9.2.5）。 */
        private fun consumeLeftovers() {
            val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
                val actions = synchronized(handler) { handler.onUnconsumedPurchasesFound(purchases.map(::toDonationPurchase)) }
                perform(actions, fromPurchaseFlow = false)
            }
        }

        private fun perform(
            actions: List<DonationAction>,
            fromPurchaseFlow: Boolean,
        ) {
            for (action in actions) {
                when (action) {
                    is DonationAction.Consume -> {
                        client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(action.purchaseToken).build()) { _, _ -> }
                        // 補做 consume 的那筆，付款當下多半已經記過了；只在這次付款流程裡記一次。
                        if (fromPurchaseFlow) analyticsTracker.track(AnalyticsEvent.DonationCompleted(action.tier))
                    }
                    is DonationAction.ShowMessage -> messageFlow.tryEmit(action.message)
                }
            }
        }

        private fun classify(result: BillingResult): DonationBillingResult =
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> DonationBillingResult.Ok
                BillingClient.BillingResponseCode.USER_CANCELED -> DonationBillingResult.UserCanceled
                else -> DonationBillingResult.Unavailable
            }

        private fun toDonationPurchase(purchase: Purchase) =
            DonationPurchase(
                purchaseToken = purchase.purchaseToken,
                productIds = purchase.products,
                state =
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> DonationPurchase.State.PURCHASED
                        Purchase.PurchaseState.PENDING -> DonationPurchase.State.PENDING
                        else -> DonationPurchase.State.UNSPECIFIED
                    },
            )
    }
