# Android Bluetooth Manager

## 1 はじめに

### 1.1 この文書について

2023/03/18に行われた四校技術交換会において、浦和高校の使用した技術の一部を公開、説明するものである。

## 2 目的

Bluetoothを使用して、ESP32またはAndroidをマスター、ESP32をスレーブとしてロボットを動作させる。

## 3 利用した技術

### 3.1 ハードウェア

- Android端末
- プログラム書き込みに必要なもの
  - PC
  - ケーブル
  - そのほか必要なもの
- スレーブのESP32 (「[Bluetoothスレーブ](Bluetooth%20スレーブ.md)」を参照)

### 3.2 ソフトウェア

- Android Studio

### 3.3 プラグイン

- 'com.android.application' version '7.2.1'
- 'com.android.library' version '7.2.1'
- 'org.jetbrains.kotlin.android' version '1.6.20'

### 3.4 依存関係

- 'androidx.core:core-ktx:1.8.0'
- 'androidx.appcompat:appcompat:1.4.2'
- 'com.google.android.material:material:1.6.1'
- 'com.google.code.gson:gson:2.8.6'
- 'androidx.constraintlayout:constraintlayout:2.1.4'
- 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
- 'androidx.test.espresso:espresso-core:3.4.0'

## 4 手法

Androidをマスターとしてスレーブに接続する。ペアリングするタイプで汎用性を確保している。

## 5 実装

### 5.1 ソースコード

#### 5.1.1 BluetoothManager.kt

```kotlin
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
```

### 5.2 解説

#### 5.2.1 getPairedDevices

ペアリングしたデバイスの一覧を取得する。

#### 5.2.2 connect

- bluetoothSocketを作成する。
- 接続する。
- エラーがあればそれに応じたトーストを表示する。

#### 5.2.3 sendByteArray

- byteArrayを送信する。
- エラーがあればそれに応じたトーストを表示する。

#### 5.2.4 setRemoteDevice

引数のMACアドレスからデバイスを設定する。
