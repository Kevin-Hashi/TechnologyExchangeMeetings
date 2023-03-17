package jp.gr.java_conf.hashiguchi_homemade.robot_bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.widget.Toast
import java.io.OutputStream
import java.util.*

class BluetoothManager(private val context: MainActivity) {
    @Suppress("DEPRECATION")
    val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var outputStream: OutputStream
    fun checkBlueToothNotAvailable(): Boolean {
        return false
    }

    fun checkBlueToothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    fun getPairedDevices(): MutableSet<BluetoothDevice> {
        context.checkRequirePermission()
        return bluetoothAdapter.bondedDevices
    }

    @SuppressLint("SetTextI18n")
    fun connect(uuid: UUID) {
        context.checkRequirePermission()
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
        } catch (e: Exception) {
            Toast.makeText(context, "BluetoothSocketの作成に失敗しました", Toast.LENGTH_SHORT).show()
            context.binding.statusView.text = "BluetoothSocketの作成に失敗しました"
            e.printStackTrace()
        }
        if (::bluetoothSocket.isInitialized) {
            try {
                bluetoothSocket.connect()
                Toast.makeText(context, "接続に成功しました", Toast.LENGTH_SHORT).show()
                context.binding.statusView.text = "接続に成功しました"
            } catch (e: Exception) {
                try {
                    bluetoothSocket.close()
                } catch (e: Exception) {
                    Toast.makeText(context, "BluetoothSocketのクローズに失敗しました", Toast.LENGTH_LONG).show()
                    context.binding.statusView.text = "BluetoothSocketのクローズに失敗しました"
                    e.printStackTrace()
                    return
                }
                Toast.makeText(context, "BluetoothSocketの接続に失敗しました", Toast.LENGTH_SHORT).show()
                context.binding.statusView.text = "BluetoothSocketの接続に失敗しました"
                e.printStackTrace()
            }
            try {
                outputStream = bluetoothSocket.outputStream
            } catch (e: Exception) {
                Toast.makeText(context, "OutputStreamの作成に失敗しました", Toast.LENGTH_SHORT).show()
                context.binding.statusView.text = "OutputStreamの作成に失敗しました"
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun sendByteArray(byteArray: ByteArray) {
        try {
            outputStream.write(byteArray)
        } catch (e: Exception) {
            try {
                context.binding.statusView.text = "送信に失敗しました"
                bluetoothSocket.close()
            } catch (e: Exception) {
                if (::bluetoothDevice.isInitialized) {
                    //Toast.makeText(context, "BluetoothSocketのクローズに失敗しました", Toast.LENGTH_SHORT).show()
                    context.binding.statusView.text = "BluetoothSocketのクローズに失敗しました"
                    e.printStackTrace()
                }
                return
            }
            //Toast.makeText(context, "送信に失敗しました", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun setRemoteDevice(macAddress: String) {
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
    }
}