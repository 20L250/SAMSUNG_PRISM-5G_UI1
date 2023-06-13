package com.example.myapplication

import android.Manifest
import android.os.Bundle
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.net.ConnectivityManager
import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.telephony.CellInfo
import android.widget.Toast
import android.os.Handler


class MainActivity : AppCompatActivity() {
    private lateinit var telephonyManager: TelephonyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        requestPermission()
        registerNetworkCallback()

    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun displayNetworkStatus(serviceState: ServiceState) {
        val networkStatus: String = when (serviceState.state) {
            ServiceState.STATE_IN_SERVICE -> "In Service"
            ServiceState.STATE_OUT_OF_SERVICE -> "Out of Service"
            ServiceState.STATE_EMERGENCY_ONLY -> "Emergency Only"
            ServiceState.STATE_POWER_OFF -> "Power Off"
            else -> "Unknown"
        }

        val textView = findViewById<TextView>(R.id.SERVICE)
        textView.text = networkStatus
    }



    private fun displayMCGInformation(cellInfoList: List<CellInfo>?){
        val mcgInfo=findViewById<TextView>(R.id.MCG)
        if (!cellInfoList.isNullOrEmpty()) {
            val cellInfo = cellInfoList[1] // Assuming you want to display information from the first cell
            val cellIdentityLte = cellInfo.cellIdentity
            val cellSignalStrengthLte = cellInfo.cellSignalStrength
            val cellIdentityString = cellIdentityLte.toString()

            val mccRegex = Regex("mMcc=(\\d+)")
            val mncRegex = Regex("mMnc=(\\d+)")
            val pciRegex = Regex("mPci=(\\d+)")
            val mcc = mccRegex.find(cellIdentityString)?.groupValues?.get(1)
            val mnc = mncRegex.find(cellIdentityString)?.groupValues?.get(1)
            val pci = pciRegex.find(cellIdentityString)?.groupValues?.get(1)
            mcgInfo.text = "MCC: $mcc  MNC: $mnc  PCI: $pci"
        }
        else{
            mcgInfo.text="Not a valid information"

        }
    }



    private fun mGetNetworkClass(context: Context): String? {

        // ConnectionManager instance
        val mConnectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val mInfo = mConnectivityManager.activeNetworkInfo

        // If not connected, "-" will be displayed
        if (mInfo == null || !mInfo.isConnected) return "-"

        // If Connected to Wifi
        if (mInfo.type == ConnectivityManager.TYPE_WIFI) return "WIFI"

        // If Connected to Mobile
        if (mInfo.type == ConnectivityManager.TYPE_MOBILE) {
            return when (mInfo.subtype) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_GSM -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN, 19 -> "4G"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "?"
            }
        }
        return "?"
    }

    private fun estimateThroughput(context: Context):Pair<Float, Float> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the active network
        val activeNetwork: Network? = connectivityManager.activeNetwork

        // Get the network capabilities of the active network
        val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(activeNetwork)

        var downlinkMbps = 0f
        var uplinkMbps = 0f

        // Check if the network capabilities are available and include TRANSPORT_CELLULAR
        if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // Get the maximum downlink and uplink speeds in bps (bits per second)
            val maxDownlinkSpeed: Long = networkCapabilities.linkDownstreamBandwidthKbps.toLong()
            val maxUplinkSpeed: Long = networkCapabilities.linkUpstreamBandwidthKbps.toLong()

            // Calculate the theoretical throughput in Mbps (megabits per second)
            downlinkMbps = maxDownlinkSpeed / 1024f
            uplinkMbps = maxUplinkSpeed / 1024f

        }

        return Pair(downlinkMbps, uplinkMbps)
    }
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    private fun registerNetworkCallback() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                updateNetworkInfo()
                // Network is available
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                Toast.makeText(applicationContext,"No Network Connected !!",Toast.LENGTH_SHORT).show()
                // Network is lost

            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }



    private fun updateNetworkInfo() {
        try {
            val textView = findViewById<TextView>(R.id.NWTYPE)
            val serviceState = telephonyManager.serviceState
            if (serviceState != null) {
                displayNetworkStatus(serviceState)
            }
            val message = mGetNetworkClass(applicationContext)
            textView.text = message
            val throughputTextView = findViewById<TextView>(R.id.TPUT)
            var handler = Handler()
            var runnable = object : Runnable {
                override fun run() {
                    val cellInfoList = telephonyManager.allCellInfo
                    displayMCGInformation(cellInfoList)
                    handler.postDelayed(this, 5000) // Refresh every 5 seconds
                }
            }

            // Start periodic cell information update
            handler.postDelayed(runnable, 5000)
            val throughput = estimateThroughput(this)
            val downlinkMbps = throughput.first
            val uplinkMbps = throughput.second

            throughputTextView.text = " Downlink: $downlinkMbps Mbps\n Uplink: $uplinkMbps Mbps"
        } catch (_: SecurityException) {

        }
    }
}







