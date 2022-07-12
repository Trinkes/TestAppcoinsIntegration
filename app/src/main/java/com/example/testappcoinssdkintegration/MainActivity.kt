package com.example.testappcoinssdkintegration

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.appcoins.sdk.billing.*
import com.appcoins.sdk.billing.helpers.CatapultBillingAppCoinsFactory
import com.appcoins.sdk.billing.listeners.AppCoinsBillingStateListener
import com.appcoins.sdk.billing.listeners.ConsumeResponseListener
import com.appcoins.sdk.billing.types.SkuType
import com.example.testappcoinssdkintegration.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity(), TestBilling {
  companion object {
    private val TAG = MainActivity::class.java.simpleName
  }

  var base64EncodedPublicKey =
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw+Qen4gtCi1triCxdb2svOsZ7OKJu19cjVDO2MNuvSw69Q+RQGYUYXQLfoFM6Nzk7tlbhIKjN2YC7/ZoxDFTzoHh1WNbLIn1X+D5p33fAUgd6vzrJLG0eLQ4A5MmXCAnaalCpgeA3BoRfmUr+ED09ycRXEhS/IU12bTN0pO3trm28SOGAkwBCsGYPBBlPvpRiydhvrcIbpfB3oEMrjvlpXo+9k8mSO0qk03gnrGp5N6inQe6k8qDQ+sdFQYPsdiDo//EGyzLwoLBEzg31DSObDJprz7rgHR2z1lDPkHf7NALD8/a49cS21SPX3IPY+YX9NI8HEALh/eiRjhQmqO5wwIDAQAB"

  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityMainBinding
  lateinit var token: String
  private val SKU = "gas"


  var purchaseFinishedListener =
    PurchasesUpdatedListener { responseCode: Int, purchases: List<Purchase> ->
      if (responseCode == ResponseCode.OK.value) {
        for (purchase in purchases) {
          token = purchase.token
          Log.d(TAG, "purchase: ${purchase.token}")
        }
      } else {
        AlertDialog.Builder(this).setMessage(
          String.format(
            Locale.ENGLISH, "response code: %d -> %s", responseCode,
            ResponseCode.values()[responseCode].name
          )
        )
          .setPositiveButton(android.R.string.ok) { dialog, which -> dialog.dismiss() }
          .create()
          .show()
      }
    }

  val appCoinsBillingStateListener: AppCoinsBillingStateListener =
    object : AppCoinsBillingStateListener {
      override fun onBillingSetupFinished(responseCode: Int) {
        if (responseCode != ResponseCode.OK.value) {
          Toast.makeText(
            baseContext,
            "Problem setting up in-app billing: $responseCode",
            Toast.LENGTH_SHORT
          )
          return
        }
        consumePurchases()
        Log.d(TAG, "Setup successful. Querying inventory. response code: $responseCode")
      }

      override fun onBillingServiceDisconnected() {
        Log.d("Message: ", "Disconnected")
      }
    }


  override fun consumePurchases() {
    Log.d(TAG, "consumePurchases() called")
    val queryPurchasesResponse = cab.queryPurchases(SkuType.inapp.toString())
    Log.d(TAG, "consumePurchases: $queryPurchasesResponse")
    val queryPurchases = queryPurchasesResponse.purchases
    for (purchase in queryPurchases) {
      cab.consumeAsync(purchase.token,
        object : ConsumeResponseListener {
          override fun onConsumeResponse(responseCode: Int, purchaseToken: String?) {
            Log.d(
              TAG,
              "onConsumeResponse() called with: responseCode = [$responseCode], purchaseToken = [$purchaseToken]"
            )
          }
        })
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  private lateinit var cab: AppcoinsBillingClient

  override fun onCreate(savedInstanceState: Bundle?) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    super.onCreate(savedInstanceState)

    cab = CatapultBillingAppCoinsFactory.BuildAppcoinsBilling(
      this,
      base64EncodedPublicKey,
      purchaseFinishedListener
    )

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    val navController = findNavController(R.id.nav_host_fragment_content_main)
    appBarConfiguration = AppBarConfiguration(navController.graph)
    setupActionBarWithNavController(navController, appBarConfiguration)

    binding.fab.setOnClickListener { view ->
      makePurchase()
    }

    cab.startConnection(appCoinsBillingStateListener)

  }

  override fun makePurchase() {
    Log.d(TAG, "Launching purchase flow.")
    val payloadDeveloper = "payloadDeveloper"
    val origin = "BDS"
    val billingFlowParams = BillingFlowParams(
      SKU, SkuType.inapp.toString(),  //Your sku type
      "orderId=" + System.currentTimeMillis(),
      payloadDeveloper,
      origin
    )

    if (!cab.isReady()) {
      cab.startConnection(appCoinsBillingStateListener)
    }

    val response: Int = cab.launchBillingFlow(this, billingFlowParams)
    Log.d(TAG, "onCreate: response: $response")
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    Log.d(
      TAG,
      "onActivityResult() called with: requestCode = [$requestCode], resultCode = [$resultCode], data = [$data]"
    )
    cab.onActivityResult(requestCode, resultCode, data)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item.itemId) {
      R.id.action_settings -> true
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    return navController.navigateUp(appBarConfiguration)
        || super.onSupportNavigateUp()
  }
}