package shop.devosify.riya.service

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemMonitorService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class SystemState(
        val batteryLevel: Int,
        val isCharging: Boolean,
        val bluetoothDevices: List<BluetoothDevice>,
        val wifiConnected: Boolean,
        val mobileDataConnected: Boolean
    )

    data class BluetoothDevice(
        val name: String,
        val batteryLevel: Int?,
        val isConnected: Boolean
    )

    fun getCurrentState(): SystemState {
        return SystemState(
            batteryLevel = getBatteryLevel(),
            isCharging = isCharging(),
            bluetoothDevices = getConnectedBluetoothDevices(),
            wifiConnected = isWifiConnected(),
            mobileDataConnected = isMobileDataConnected()
        )
    }

    private fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, 
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else 0
    }

    private fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, 
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || 
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getConnectedBluetoothDevices(): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.bondedDevices?.map { device ->
            BluetoothDevice(
                name = device.name,
                batteryLevel = getBatteryLevel(device), // Requires additional implementation
                isConnected = device.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED
            )
        } ?: emptyList()
    }
} 