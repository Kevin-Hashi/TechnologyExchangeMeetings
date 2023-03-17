# Android Bluetooth バイト通信

## 1 はじめに

### 1.1 この文書について

2023/03/18に行われた四校技術交換会において、浦和高校の使用した技術の一部を公開、説明するものである。

## 2 目的

Bluetoothを使用して通信を行う際に、バイト列をやり取りすることで通信時間を短くし、応答性を向上させる。
JSONなどの文字列を使用すると、とくに小数が含まれる場合などに応答性が著しく低下する。送られる情報が大きく、前の情報が途中までしか送られないことに起因する。

## 3 利用した技術

### 3.1 ハードウェア

- Android端末
- プログラム書き込みに必要なもの
  - PC
  - ケーブル
  - そのほか必要なもの

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

通信では3バイトの情報を送信する。

### 4.1 バイト列

#### 4.1.1 早見表

1バイト目
| ビット |   1   |   2   |   3   |   4   |        5         |       6       |       7        |       8        |
| ------ | :---: | :---: | :---: | :---: | :--------------: | :-----------: | :------------: | :------------: |
| 情報   |   0   |   >   |   >   |   >   | チェックデジット | LEDのオンオフ | 左モータの向き | 右モータの向き |

2バイト目
| ビット |   1   |   2   |   3   |   4   |   5   |   6   |   7   |        8         |
| ------ | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--------------: |
| 情報   |   >   |   >   |   >   |   >   |   >   |   >   |   >   | 左モータの回転量 |

3バイト目
| ビット |   1   |   2   |   3   |   4   |   5   |   6   |   7   |        8         |
| ------ | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--------------: |
| 情報   |   >   |   >   |   >   |   >   |   >   |   >   |   >   | 右モータの回転量 |

#### 4.1.2 バイト列の解説

- 1バイト目
  - 1
    - 未使用
  - 2 3 4 5
    - チェックデジット
    - 0-15の範囲
  - 6
    - LEDのオンオフ
    - trueで点灯させる
    - 1回送るだけでよい
    - 未使用
  - 7 8
    - 左右のモータの向き
    - trueで逆転
- 2,3バイト目
  - 左右のモータの回転量
  - 256段階で調整する

## 5 実装

### 5.1 ソースコード

#### 5.1.1 ByteManager.kt

```kotlin
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
```

### 5.2 解説

#### 5.2.1 setLED, setMotorDirection系

引数が`true`なら、それが入るビットを1にする。
`false`なら0にする。

`setLED`は未使用。

#### 5.2.2 setMotorSpeed系

引数をそのまま2,3バイト目に入れる。
引数は`uByte`。

#### 5.2.3 calcCheckDigit

計算する1バイト目と2,3バイト目を`uByteArray`にして`checkD`に渡して返す。

#### 5.2.4 setCheckDigits

1バイト目の下3ビットを`calcCheckDigits`に渡して計算する。
計算した値は3ビット左シフトして1ビット目に入る。

#### 5.2.5 checkD

整数列からチェックデジットを作成する。
それぞれに1,3,5,... 倍した和を16で割った余りである。
4bitになる。
返り値は`UByte`。
