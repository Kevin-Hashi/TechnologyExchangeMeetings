# Android Bluetooth マスター

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

### 3.5 ライブラリ

- ByteManager (「[AndroidBluetoothバイト通信](Android%20Bluetooth%20バイト通信.md)」を参照)
- BluetoothManager (「[AndroidBluetoothManager](Android%20Bluetooth%20Manager.md)」を参照)

## 4 手法

ペアリング済みの端末を取得、選択し接続する。バイト列を送信する。

## 5 実装

### 5.1 ソースコード

めちゃくちゃ抜粋して一部を掲載する。
操作画面は各々作ってほしい。ソースコードと「[Android操作画面](Android%20操作画面.md)」を参考に。
パーミッション関連はうまいこと作ってほしい。ソースコードを参考に。

#### 5.1.1 MainActivity.kt

```kotlin
class MainActivity {
    private val myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val byteManager = ByteManager()
    private val bluetoothManager = BluetoothManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (bluetoothManager.checkBlueToothNotAvailable()) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Bluetoothは利用不可能です", Toast.LENGTH_LONG).show()
        }
        if (!bluetoothManager.checkBlueToothEnabled()) {
            Toast.makeText(this, "Bluetoothを有効にしてください", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        val pairedDevices = bluetoothManager.getPairedDevices()
        val devices: MutableList<List<String>> = mutableListOf()
        pairedDevices.forEach { device ->
            devices.add(listOf(device.name, device.address))
        }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice)
        devices.forEach { device ->
            adapter.add(device[0])
        }
        createDialogForSelectDevice(adapter, devices)
    }

    private fun createDialogForSelectDevice(
        adapter: ArrayAdapter<String>, devices: MutableList<List<String>>
    ) {
        val builder = AlertDialog.Builder(this)
        var alertDialog: AlertDialog? = null
        val onDialogClickListener = DialogInterface.OnClickListener { _, which ->
            Toast.makeText(this, "接続中...", Toast.LENGTH_SHORT).show()
            bluetoothManager.setRemoteDevice(devices[which][1])
            connect()
            alertDialog?.dismiss()
        }
        builder.setTitle("Bluetoothデバイスを選択してください")
        builder.setSingleChoiceItems(adapter, -1, onDialogClickListener)
        alertDialog = builder.create()
        alertDialog.show()
    }

    private fun send(): ByteArray {
        val array = ByteArray(byteManager.arraySize)
        for ((index, elem) in byteManager.array.withIndex()) {
            array[index] = elem.toByte()
        }
        bluetoothManager.sendByteArray(array)
        return array
    }

    private fun connect() {
        binding.textView.text = bluetoothManager.bluetoothDevice.address
        bluetoothManager.connect(myUUID)
    }
}
```

### 5.2 解説

#### 5.2.1 myUUID

Bluetooth通信の際のUUIDは決まっている。
SPPのUUIDは"00001101-0000-1000-8000-00805F9B34FB"である。

#### 5.2.2 byteManager, bluetoothManager

「[AndroidBluetoothバイト通信](Android%20Bluetooth%20バイト通信.md)」、「[AndroidBluetoothManager](Android%20Bluetooth%20Manager.md)」を参照

#### 5.2.3 onCreate

- デバイスでBluetoothが使用できるか判定
- Bluetoothが有効かどうか判定。無効なら設定画面に飛ばす。
- ペアリングしたデバイスから、使用するデバイスを選択する。`createDialogForSelectDevice`を呼ぶ

#### 5.2.4 createDialogForSelectDevice

デバイスを選択させ、接続する。

#### 5.2.5 send

ByteManagerのarrayを送信する。

#### 5.2.6 connect

`bluetoothManager.connect`を呼び出す。myUUIDを渡す。
