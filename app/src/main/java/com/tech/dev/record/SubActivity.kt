package com.tech.dev.record;


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.android.billingclient.api.*

import kotlinx.android.synthetic.main.activity_sub.*

class SubActivity : AppCompatActivity(), PurchasesUpdatedListener {
    private lateinit var billingClient: BillingClient
    private lateinit var listenner: ConsumeResponseListener
    private val sku1 = BillingClientSetup.ITEM_TO_BUY_SKU_1
    private val sku2 = BillingClientSetup.ITEM_TO_BUY_SKU_2
    private val sku3 = BillingClientSetup.ITEM_TO_BUY_SKU_3

    private lateinit var skuDetailsParams: SkuDetailsParams
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)
        setupBillingClient()
        skuDetailsParams = SkuDetailsParams.newBuilder().setSkusList(listOf(sku1, sku2, sku3))
            .setType(BillingClient.SkuType.SUBS)
            .build()
        relative1.setOnClickListener {
            launchBilling(sku1)
        }
        relative2.setOnClickListener {
            launchBilling(sku2)
        }
        relative3.setOnClickListener {
            launchBilling(sku3)
        }
    }

    private fun launchBilling(sku: String) {
        if (billingClient.isReady) {
            billingClient.querySkuDetailsAsync(
                skuDetailsParams,
                object : SkuDetailsResponseListener {
                    override fun onSkuDetailsResponse(
                        p0: BillingResult,
                        p1: MutableList<SkuDetails>?
                    ) {
                        if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                            handleBilling(p1?.find { it.sku == sku }!!)
                        } else {
                        }
                    }
                })
        }
    }
    private fun handleBilling(sku: SkuDetails) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(sku)
            .build()
        val response = billingClient.launchBillingFlow(this, billingFlowParams).responseCode
        when (response) {
            BillingClient.BillingResponseCode.OK -> {
                showNotice("mua thanh cong")
            }
            else -> {
                showNotice("loi roi")
            }
        }

    }

    fun setupBillingClient() {
        listenner = object : ConsumeResponseListener {
            override fun onConsumeResponse(p0: BillingResult, p1: String) {
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    showNotice("Consume OK!")
                }
            }

        }
        billingClient = BillingClientSetup.getInstance(this, this)
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                showNotice("mat ket noi")
            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    showNotice("ket noi thanh cong!")
                    //query
                    val purchase = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                        .purchasesList
                    handleArlreadyPurchase(purchase!!)
                } else {
                    showNotice("ket noi ko thanh cong")
                }
            }

        })
    }

    fun handleArlreadyPurchase(purchases: List<Purchase>) {
        showNotice("size=" + purchases.size)
        for (purchase: Purchase in purchases) {
            if (purchase.skus.indexOf(sku1) > 0 || purchase.skus.indexOf(sku2) > 0 || purchase.skus.indexOf(
                    sku3
                ) > 0
            ) {
                val consumParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.consumeAsync(consumParams, listenner)
            }
        }
    }

    private fun showNotice(content: String) {
        Toast.makeText(this, content, Toast.LENGTH_LONG).show()
    }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        if (p0.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
            for (purchase in p1) {
                handlePurchase(p1)
            }
        } else if (p0.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    private fun handlePurchase(p1: MutableList<Purchase>) {
        for (purchase in p1) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(
                        acknowledgePurchaseParams
                    ) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Toast.makeText(applicationContext, "Subs Success!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }
}

