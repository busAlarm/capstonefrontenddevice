package com.example.busarrivalalram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log

class WifiBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "WifiBroadcastReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (networkInfo != null && networkInfo.isConnected) {
                // Wi-Fi 연결 상태가 변경되었으며 연결된 상태인 경우
                val ssid = wifiManager.connectionInfo.ssid
                Log.d(TAG, "Connected to $ssid")

                // 특정 SSID를 확인하여 연결 여부를 판단
                if (ssid == "DKU_WiFi") {
                    // 특정 SSID로 연결되었으므로 추가 작업 수행
                    // 예를 들어, 연결이 끊어진 경우 다시 해당 SSID로 연결하는 로직을 추가할 수 있습니다.
                    reconnectToSpecificSSID(context, wifiManager)
                }
            }
        }
    }

    private fun reconnectToSpecificSSID(context: Context, wifiManager: WifiManager) {
        val ssid = "32184893"
        val password = "vegasorg12@"

        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = String.format("\"%s\"", ssid)
        wifiConfiguration.preSharedKey = String.format("\"%s\"", password)

        val networkId = wifiManager.addNetwork(wifiConfiguration)
        wifiManager.disconnect()
        wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()
    }
}
