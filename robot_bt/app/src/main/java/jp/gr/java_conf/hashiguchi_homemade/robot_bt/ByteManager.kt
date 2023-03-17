package jp.gr.java_conf.hashiguchi_homemade.robot_bt

import android.util.Log

private const val TAG = "ByteManager"

@ExperimentalUnsignedTypes
class ByteManager {
    val arraySize = 3
    val array: UByteArray = UByteArray(arraySize)

    fun setLED(led: Boolean) {
        this.array[0] = if (led) this.array[0] or 0b00000100u else this.array[0] and 0b11111011u
    }

    fun setLeftMotorDirection(back: Boolean) {
        this.array[0] = if (back) this.array[0] or 0b00000010u else this.array[0] and 0b11111101u
    }

    fun setRightMotorDirection(back: Boolean) {
        this.array[0] = if (back) this.array[0] or 0b00000001u else this.array[0] and 0b11111110u
    }

    fun setLeftMotorSpeed(speed: UByte) {
        this.array[1] = speed
    }

    fun setRightMotorSpeed(speed: UByte) {
        this.array[2] = speed
    }

    private fun calcCheckDigits(firstByte: UByte): UByte {
        val checkList = UByteArray(3)
        checkList[0] = firstByte
        checkList[1] = this.array[1]
        checkList[2] = this.array[2]
        Log.i(TAG, "checkDigit:${checkD(checkList)}")
        return checkD(checkList)
    }

    fun setCheckDigits() {
        val firstByte: UByte = this.array[0] and 0b00000111u
        var calcFirstByte = this.calcCheckDigits(firstByte).toUInt()
        calcFirstByte = calcFirstByte shl 3
        calcFirstByte = calcFirstByte or firstByte.toUInt()
        this.array[0] = calcFirstByte.toUByte()
    }

    private fun checkD(uByteArray: UByteArray): UByte {
        var bias = 1
        var sum = 0
        for (ub in uByteArray) {
            sum += ub.toInt() * bias
            bias += 2
        }
        return (sum % 16).toUByte()
    }
}