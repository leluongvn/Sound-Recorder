package com.tech.dev.record;


import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

object BillingClientSetup {
    private val instance: BillingClient? = null
    fun getInstance(context: Context, listener: PurchasesUpdatedListener): BillingClient {
        return instance ?: setupBillingClient(context, listener)
    }
    private fun setupBillingClient(
        context: Context,
        listener: PurchasesUpdatedListener
    ): BillingClient {
        return BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(listener)
            .build()
    }
    const val ITEM_TO_BUY_SKU_1 = "buyapp1"
    const val ITEM_TO_BUY_SKU_2 = "buyapp2"
    const val ITEM_TO_BUY_SKU_3 = "buyapp3"
}