package s.upipayment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*


/**
 * Transfer Money through UPI Id
 */

class MainActivity : AppCompatActivity() {

    // values from UI
    private lateinit var amount: EditText
    private lateinit var remarks: EditText
    private lateinit var name: EditText
    private lateinit var upiId: EditText
    private lateinit var sendNowButton: Button

    private val upiPayment = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize views
        sendNowButton = findViewById(R.id.send)
        amount = findViewById(R.id.amount_et)
        remarks = findViewById(R.id.note)
        name = findViewById(R.id.name)
        upiId = findViewById(R.id.upi_id)

        //send now button click implementation
        sendNowButton.setOnClickListener {
            val amount = amount.text.toString()
            val note = remarks.text.toString()
            val name = name.text.toString()
            val upiId = upiId.text.toString()
            payUsingUpi(amount, upiId, name, note)
        }
    }

    //payment via UPI method
    private fun payUsingUpi(amount: String, upiId: String, name: String, note: String) {

        //URL build with query and its value (CURRENCY : INR)
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        // start payment from existing app
        val upiPayIntent = Intent(Intent.ACTION_VIEW)
        upiPayIntent.data = uri

        // chooser dialog to pay via all UPI app available in system
        val chooser = Intent.createChooser(upiPayIntent, resources.getString(R.string.pay_with))

        // check if intent resolves or not
        if (null != chooser.resolveActivity(packageManager)) {
            startActivityForResult(chooser, upiPayment)
        } else {
            Toast.makeText(
                this,
                resources.getString(R.string.no_upi_app_found_error),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            upiPayment -> if (Activity.RESULT_OK == resultCode || resultCode == 11) {
                if (data != null) {
                    val trxt = data.getStringExtra("response")
                    val dataList = ArrayList<String>()
                    dataList.add(trxt!!)
                    upiPaymentDataOperation(dataList)
                } else {
                    val dataList = ArrayList<String>()
                    dataList.add("nothing")
                    upiPaymentDataOperation(dataList)
                }
            } else {
                val dataList = ArrayList<String>()
                dataList.add("nothing")
                upiPaymentDataOperation(dataList)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun upiPaymentDataOperation(data: ArrayList<String>) {
        if (isConnectionAvailable(this)) {
            var str: String? = data[0]
            var paymentCancel = ""
            if (str == null) str = "discard"
            var status = ""
//            var approvalRefNo = ""
            val response = str.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in response.indices) {
                val equalStr =
                    response[i].split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (equalStr.size >= 2) {
                    if (equalStr[0].toLowerCase() == "Status".toLowerCase()) {
                        status = equalStr[1].toLowerCase()
                    }
//                    else if (equalStr[0].toLowerCase() == "ApprovalRefNo".toLowerCase() || equalStr[0].toLowerCase() == "txnRef".toLowerCase()) {
//                        approvalRefNo = equalStr[1]
//                    }
                } else {
                    paymentCancel = resources.getString(R.string.payment_cancelled_by_user)
                }
            }

            when {
                status == "success" -> //Code to handle successful transaction here.
                    Toast.makeText(
                        this,
                        resources.getString(R.string.payment_success),
                        Toast.LENGTH_SHORT
                    ).show()
                resources.getString(R.string.payment_cancelled_by_user) == paymentCancel -> Toast.makeText(
                    this,
                    resources.getString(R.string.payment_cancelled_by_user),
                    Toast.LENGTH_SHORT
                ).show()
                else -> Toast.makeText(
                    this,
                    resources.getString(R.string.transaction_failed),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        } else {
            Toast.makeText(
                this,
                resources.getString(R.string.no_internet_available_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        //to check network availability with system
        fun isConnectionAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            // for system version below SDK version 23
            if (Build.VERSION.SDK_INT < 23) {
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null) {
                    return networkInfo.isConnected && (networkInfo.type == ConnectivityManager.TYPE_WIFI || networkInfo.type == ConnectivityManager.TYPE_MOBILE)
                }
            }
            // for system version above SDK version 23
            else {
                val network = connectivityManager.activeNetwork
                if (network != null) {
                    val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                    return networkCapabilities!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    )
                }
            }

            //default return false
            return false
        }
    }
}