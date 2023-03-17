package jp.gr.java_conf.hashiguchi_homemade.robot_bt

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import jp.gr.java_conf.hashiguchi_homemade.robot_bt.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2

private const val REQUEST_BLUETOOTH = 0
private const val REQUEST_BLUETOOTH_ADMIN = 1
private const val REQUEST_CONNECT_BT = 2
private const val REQUEST_SCAN_BT = 3

private const val TAG = "MainActivity"

private fun showLog(pre: String = "", array: ByteArray, post: String = "") {
    Log.i(TAG, "size:${array.size}")
    Log.i(TAG, "array[0]:${array[0].toString(2)}")
    var str = pre
    array.forEach { str += " ${it.toUByte()}" }
    str += post
    Log.i(TAG, str)
}


@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), OnTouchListener {
    lateinit var binding: ActivityMainBinding
    private lateinit var data: SharedPreferences
    private var preDx = BigDecimal.ZERO
    private var preDy = BigDecimal.ZERO
    private var originX = BigDecimal.ZERO
    private var originY = BigDecimal.ZERO
    private var sqDistance = BigDecimal.ZERO
    private var displaySqSize40per = BigDecimal.ZERO
    private var topBarHeight = 0

    private var fixedOrigin = false

    private val myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val byteManager = ByteManager()

    private val maxValue = 255

    private val bluetoothManager = BluetoothManager(this)
    private val motorControl = MotorControl()

    private val switchKey = "fixed_switch_last"

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val leftRight = when (seekBar?.id) {
                binding.seekBar1.id -> 0
                binding.seekBar2.id -> 1
                else -> throw Exception("This lister is must use for seekbar!")
            }
            if (leftRight == 0) {
                byteManager.setLeftMotorSpeed((abs(progress) * maxValue / 5).toUByte())
                byteManager.setLeftMotorDirection(progress < 0)
                val right = when (leftRight) {
                    0 -> binding.seekBar2
                    1 -> binding.seekBar1
                    else -> throw Exception("This lister is must use for seekbar!")
                }.progress
                byteManager.setRightMotorSpeed((abs(right) * maxValue / 5).toUByte())
                byteManager.setRightMotorDirection(right < 0)
            } else {
                byteManager.setRightMotorSpeed((abs(progress) * maxValue / 5).toUByte())
                byteManager.setRightMotorDirection(progress < 0)
                val left = when (leftRight) {
                    0 -> binding.seekBar2
                    1 -> binding.seekBar1
                    else -> throw Exception("This lister is must use for seekbar!")
                }.progress
                byteManager.setLeftMotorSpeed((abs(left) * maxValue / 5).toUByte())
                byteManager.setLeftMotorDirection(left < 0)
            }
            byteManager.setCheckDigits()
            val sendData = send()
            showLog("array", sendData)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            //TODO("Not yet implemented")
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            seekBar?.progress = 0
            val leftRight = when (seekBar?.id) {
                binding.seekBar1.id -> 0
                binding.seekBar2.id -> 1
                else -> throw Exception("This lister is must use for seekbar!")
            }
            if (leftRight == 0) {
                byteManager.setLeftMotorSpeed(0u)
                byteManager.setLeftMotorDirection(false)
            }
            byteManager.setCheckDigits()
            val sendData = send()
            showLog("array", sendData, " end!")
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        data = getSharedPreferences("Data", MODE_PRIVATE)
        binding.touchView.setOnTouchListener(this)
        checkRequirePermission()
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
        Log.i(TAG, "pairedDevices: $devices")
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice)
        devices.forEach { device ->
            adapter.add(device[0])
        }
        binding.connectButton.setOnClickListener { createDialogForSelectDevice(adapter, devices) }
        binding.oSwitch.setOnCheckedChangeListener { _, isChecked ->
            fixedOrigin = isChecked
            data.edit().putBoolean(switchKey, isChecked).commit()
            binding.biasSelector.visibility = if (isChecked) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }
        binding.oSwitch.isChecked = data.getBoolean(switchKey, false)
        fixedOrigin = binding.oSwitch.isChecked
        binding.biasSelector.visibility = if (fixedOrigin) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        binding.biasSelector.check(binding.biasLinear.id)
        binding.controlSystemGroup.setOnCheckedChangeListener { _, _ -> selectControlSystem() }
        binding.controlSystemGroup.check(binding.panelControl.id)
        binding.seekBar1.setOnSeekBarChangeListener(seekBarListener)
        binding.seekBar2.setOnSeekBarChangeListener(seekBarListener)
        createDialogForSelectDevice(adapter, devices)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(dm)
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        topBarHeight = (2 * dm.heightPixels - binding.mainActivity.height - rect.bottom) / 2
        val widthPow2 = BigDecimal(dm.widthPixels).pow(2)
        val heightPow2 = BigDecimal(dm.heightPixels).pow(2)
        displaySqSize40per = widthPow2.add(heightPow2).multiply(BigDecimal("0.4"))
        Log.i("Display 40per", displaySqSize40per.toString())
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val newDx =
            BigDecimal.valueOf(event?.rawX?.toDouble() ?: 0.0).setScale(3, RoundingMode.HALF_UP)
        val newDy =
            BigDecimal.valueOf(event?.rawY?.toDouble() ?: 0.0).setScale(3, RoundingMode.HALF_UP)
        if (originX == BigDecimal.ZERO) {
            originX = newDx
        }
        if (originY == BigDecimal.ZERO) {
            originY = newDy
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (fixedOrigin) {
                    Log.i(
                        "x,y,statusBar",
                        "${originX.toInt()}, ${originY.toInt()}, $topBarHeight"
                    )
                    binding.originCircle.visibility = View.VISIBLE
                    binding.originCircle.layout(
                        originX.toInt() - binding.originCircle.width / 2,
                        originY.toInt() - topBarHeight - binding.originCircle.height / 2,
                        originX.toInt() + binding.originCircle.width / 2,
                        originY.toInt() - topBarHeight + binding.originCircle.height / 2
                    )
                }
            }
            MotionEvent.ACTION_MOVE -> {
                binding.touchView.visibility = View.INVISIBLE
                v?.performClick()
                val (dx, dy) = if (fixedOrigin) {
                    // 固定式原点
                    listOf(newDx.subtract(originX), newDy.subtract(originY).negate())
                } else {
                    // 移動式原点
                    // 左下スタートにする
                    listOf(newDx.subtract(preDx), newDy.subtract(preDy).negate())
                }
                val degree = BigDecimal.valueOf(atan2(dy.toDouble(), dx.toDouble()))
                    .multiply(BigDecimal(180))
                    .divide(BigDecimal(Math.PI.toString()), 10, RoundingMode.HALF_UP)
                Log.i(TAG, "x:$newDx, y:$newDy, dx:$dx, dy:$dy")
                Log.i(TAG, "radian:$degree")
                sqDistance = sqPythagoras(dx, dy, 3)
                val xy = motorControl.calcMotor(degree)
                val changedXY = xy.toMutableList()
                if (fixedOrigin) {
                    if (sqDistance < displaySqSize40per) {
                        for ((i, c) in changedXY.withIndex()) {
                            changedXY[i] = c.multiply(
                                sqDistance.divide(
                                    displaySqSize40per,
                                    10,
                                    RoundingMode.HALF_UP
                                ).sqrt(11)
                            )
                        }
                    }
                    fun changedXYBase(func: (BigDecimal) -> BigDecimal) {
                        for ((i, c) in changedXY.withIndex()) {
                            changedXY[i] = func(c.abs()).multiply(BigDecimal(xy[i].signum()))
                        }
                    }
                    when (binding.biasSelector.getCheckedRadioButtonId()) {
                        binding.biasSgmLn9.id -> changedXYBase(motorControl::sgmLn99)
                        binding.biasSgmLn199.id -> changedXYBase(motorControl::sgmLn199)
                        binding.biasLSgmLn99.id -> changedXYBase(motorControl::lSgmLn99)
                        binding.biasLSgmEP2.id -> changedXYBase(motorControl::lSgmEP2)
                        binding.biasFrac1.id -> changedXYBase(motorControl::rat1)
                        binding.biasFrac2.id -> changedXYBase(motorControl::rat2)
                        binding.biasFracE.id -> changedXYBase(motorControl::ratE)
                        binding.biasFrac4.id -> changedXYBase(motorControl::rat4)
                    }
                } else {
                    if (sqDistance < BigDecimal.TEN.pow(3)) {
                        for ((i, c) in changedXY.withIndex()) {
                            changedXY[i] = c.multiply(
                                sqDistance.divide(
                                    BigDecimal.TEN.pow(3),
                                    10,
                                    RoundingMode.HALF_UP
                                ).sqrt(10)
                            )
                        }
                    }
                }
                Log.i(TAG, "xy,$xy")
                Log.i(TAG, "sqDistance:$sqDistance")
                if (sqDistance > BigDecimal.ONE && sqDistance < BigDecimal.TEN.pow(3)
                        .multiply(BigDecimal(5))
                ) {
                    Log.i(TAG, "sqDistance:${sqDistance.sqrt()}")
                }
                byteManager.setLeftMotorSpeed((abs(changedXY[0].toInt()) * maxValue / 5).toUByte())
                byteManager.setLeftMotorDirection(changedXY[0].compareTo(BigDecimal.ZERO) == -1)
                byteManager.setRightMotorSpeed((abs(changedXY[1].toInt()) * maxValue / 5).toUByte())
                byteManager.setRightMotorDirection(changedXY[1].compareTo(BigDecimal.ZERO) == -1)
                Log.i(TAG, (changedXY[0] * BigDecimal(maxValue)).toInt().toString())
                byteManager.setCheckDigits()
                val sendData = send()
                showLog("array", sendData)
                Log.i(TAG, "==============================================")

                if (!fixedOrigin) {
                    preDx = newDx
                    preDy = newDy
                }
            }

            MotionEvent.ACTION_UP -> {
                binding.originCircle.visibility = View.INVISIBLE
                binding.touchView.visibility = View.VISIBLE
                preDx = BigDecimal.ZERO
                preDy = BigDecimal.ZERO
                originX = BigDecimal.ZERO
                originY = BigDecimal.ZERO
                if ((!fixedOrigin && sqDistance >= BigDecimal.TEN.pow(3)) || (fixedOrigin && sqDistance >= displaySqSize40per)) {
                    Log.i(TAG, "Flicked")
                } else {
                    byteManager.setLeftMotorSpeed(0u)
                    byteManager.setRightMotorDirection(false)
                    byteManager.setRightMotorSpeed(0u)
                    byteManager.setRightMotorDirection(false)
                    send()
                }
                Log.i(TAG, "Release")
            }
        }
        return true
    }

    private fun selectControlSystem() {
        when (binding.controlSystemGroup.checkedRadioButtonId) {
            binding.panelControl.id -> {
                binding.touchView.visibility = View.VISIBLE
                binding.selectOrigin.visibility = View.VISIBLE
                binding.biasSelector.visibility = View.VISIBLE
                binding.seekBar1.visibility = View.GONE
                binding.seekBar2.visibility = View.GONE
            }
            binding.seekControl.id -> {
                binding.touchView.visibility = View.GONE
                binding.selectOrigin.visibility = View.GONE
                binding.biasSelector.visibility = View.GONE
                binding.seekBar1.visibility = View.VISIBLE
                binding.seekBar2.visibility = View.VISIBLE
            }
        }
    }

    private fun squareEuclidDistance(
        x1: BigDecimal, y1: BigDecimal, x2: BigDecimal, y2: BigDecimal, scale: Int
    ): BigDecimal {
        val x = x1 - x2
        val y = y1 - y2
        return x.pow(2).plus(y.pow(2)).setScale(scale, RoundingMode.HALF_UP)
    }

    private fun sqPythagoras(x: BigDecimal, y: BigDecimal, scale: Int): BigDecimal {
        return squareEuclidDistance(x, y, BigDecimal.ZERO, BigDecimal.ZERO, scale)
    }

    private fun BigDecimal.sqrt(scale: Int = 5): BigDecimal {
        var x = BigDecimal(kotlin.math.sqrt(this.toDouble()), MathContext.DECIMAL64)
        if (scale < 17) return x.setScale(scale, RoundingMode.HALF_UP)
        val b2 = BigDecimal(2)
        for (tempScale in 16 until scale step 2) {
            x = x.subtract(
                x.multiply(x).subtract(this).divide(x.multiply(b2), scale, RoundingMode.HALF_UP)
            )
        }
        return x.setScale(scale, RoundingMode.HALF_UP)
    }

    private fun createDialogForSelectDevice(
        adapter: ArrayAdapter<String>, devices: MutableList<List<String>>
    ) {
        val builder = AlertDialog.Builder(this)
        var alertDialog: AlertDialog? = null
        val onDialogClickListener = DialogInterface.OnClickListener { _, which ->
            Toast.makeText(this, "接続中...", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "bluetoothDevice: ${devices[which]}")
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


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsJumpToSetting()
        }
    }

    fun checkRequirePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionNotGranted(android.Manifest.permission.BLUETOOTH)) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.BLUETOOTH), REQUEST_BLUETOOTH
                )
            }
            if (permissionNotGranted(android.Manifest.permission.BLUETOOTH_ADMIN)) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.BLUETOOTH_ADMIN), REQUEST_BLUETOOTH_ADMIN
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (permissionNotGranted(android.Manifest.permission.BLUETOOTH_SCAN)) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), REQUEST_SCAN_BT
                )
            }
            if (permissionNotGranted(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CONNECT_BT
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun permissionNotGranted(permission: String): Boolean {
        return checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsJumpToSetting() {
        Toast.makeText(this, "Bluetoothの権限を許可してください", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${packageName}")
        startActivity(intent)
    }
}