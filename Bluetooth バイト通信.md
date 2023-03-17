# Bluetooth バイト通信

## 1 はじめに

### 1.1 この文書について

2023/03/18に行われた四校技術交換会において、浦和高校の使用した技術の一部を公開、説明するものである。

## 2 目的

Bluetoothを使用して通信を行う際に、バイト列をやり取りすることで通信時間を短くし、応答性を向上させる。
JSONなどの文字列を使用すると、とくに小数が含まれる場合などに応答性が著しく低下する。送られる情報が大きく、前の情報が途中までしか送られないことに起因する。

## 3 利用した技術

### 3.1 ハードウェア

- ESP32
- ESP32の動作に必要なもの
- プログラム書き込みに必要なもの
  - PC
  - ケーブル
  - そのほか必要なもの

### 3.2 ソフトウェア

- Arduino IDE
- Visual Studio Code

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
  - 7 8
    - 左右のモータの向き
    - trueで逆転
- 2,3バイト目
  - 左右のモータの回転量
  - 256段階で調整する

## 5 実装

### 5.1 ソースコード

#### 5.1.1 check.h

```cpp
#ifndef INCLUDED_MotorDirection_
#define INCLUDED_MotorDirection_
enum class MotorDirection : bool
{
    Forward,
    Back
};
#endif
#ifndef INCLUDED_MotorByte_
#define INCLUDED_MotorByte_
class MotorByte
{
public:
    static constexpr int array_size = 3;
    uint8_t array[array_size];
    MotorByte(void);
    void setArray(uint8_t array[]);
    void setLED(bool led);
    bool getLED();
    void setLeftMotorDirection(bool back);
    void setLeftMotorDirection(MotorDirection direction);
    MotorDirection getLeftMotorDirection();
    void setRightMotorDirection(bool back);
    void setRightMotorDirection(MotorDirection direction);
    MotorDirection getRightMotorDirection();
    void setLeftMotorSpeed(uint8_t speed);
    uint8_t getLeftMotorSpeed();
    void setRightMotorSpeed(uint8_t speed);
    uint8_t getRightMotorSpeed();
    uint8_t calcCheckDigits(uint8_t first_byte);
    void setCheckDigits();
    uint8_t getCheckDigits();
};
#endif
```

#### 5.1.2 check.cpp

```cpp
#include <stdint.h>
#include "Arduino.h"
#include "check.h"
using std::vector;
uint8_t checkD(uint8_t uint8tarray[], int size)
{
    int bias = 1;
    int sum = 0;
    for (int i = 0; i < size; i++)
    {
        sum += uint8tarray[i] * bias;
        bias += 2;
    }
    return sum % 16;
}

MotorByte::MotorByte(void) : array{0} {}
void MotorByte::setArray(uint8_t array[])
{
    for (int i = 0; i < 4; i++)
    {
        this->array[i] = array[i];
    }
}
void MotorByte::setLED(bool led)
{
    if (led)
    {
        this->array[0] |= 0b00000100;
    }
    else
    {
        this->array[0] &= 0b11111011;
    }
}
void MotorByte::setLeftMotorDirection(bool back)
{
    if (back)
    {
        this->array[0] |= 0b10;
    }
    else
    {
        this->array[0] &= 0b11111101;
    }
}
void MotorByte::setLeftMotorDirection(MotorDirection direction)
{
    this->setLeftMotorDirection(static_cast<bool>(direction));
}
void MotorByte::setRightMotorDirection(bool back)
{
    if (back)
    {
        this->array[0] |= 0b1;
    }
    else
    {
        this->array[0] &= 0b11111110;
    }
}
void MotorByte::setRightMotorDirection(MotorDirection direction)
{
    this->setRightMotorDirection(static_cast<bool>(direction));
}
void MotorByte::setLeftMotorSpeed(uint8_t speed)
{
    this->array[1] = speed;
}
void MotorByte::setRightMotorSpeed(uint8_t speed)
{
    this->array[2] = speed;
}
uint8_t MotorByte::calcCheckDigits(uint8_t first_byte)
{
    uint8_t check_list[3] = {first_byte, array[1], array[2]};
    uint8_t check_digits = checkD(check_list, 3);
    return check_digits;
}
void MotorByte::setCheckDigits()
{
    uint8_t first_byte = this->array[0] & 0b00000111;
    uint8_t digited_first_byte = this->calcCheckDigi@ts(first_byte);
    digited_first_byte <<= 3;
    digited_first_byte |= first_byte;
    this->array[0] = digited_first_byte;
}
bool MotorByte::getLED()
{
    return static_cast<bool>(bitRead(this->array[0], 2));
}
MotorDirection MotorByte::getLeftMotorDirection()
{
    return static_cast<MotorDirection>(bitRead(this->array[0], 1));
}
MotorDirection MotorByte::getRightMotorDirection()
{
    return static_cast<MotorDirection>(bitRead(this->array[0], 0));
}
uint8_t MotorByte::getLeftMotorSpeed()
{
    return this->array[1];
}
uint8_t MotorByte::getRightMotorSpeed()
{
    return this->array[2];
}
uint8_t MotorByte::getCheckDigits()
{
    uint8_t digits = this->array[0];
    digits >>= 3;
    digits &= 0b1111;
    return digits;
}
```

### 5.2 解説

#### 5.2.1 setArray

引数の整数列をメンバー変数に代入する。
中のループが3回で固定になっているため、`MotorByte::array_size`による回数に書き換えたほうが良い。

#### 5.2.1 checkD

整数列からチェックデジットを作成する。
それぞれに1,3,5,... 倍した和を16で割った余りである。
4bitになる。

#### 5.2.2 setLED, setMotorDirection系

引数が`true`なら、それが入るビットを1にする。
`false`なら0にする。
`MotorDirection`型も引数にとる。

#### 5.2.3 setMotorSpeed系

引数をそのまま2,3バイト目に入れる。
引数は`uint8_t`。

#### 5.2.4 setCheckDigits

1バイト目の下3ビットを`calcCheckDigits`に渡して計算する。
計算した値は3ビット左シフトして1ビット目に入る。

#### 5.2.5 getLED, getMotorDirection系

対応するビットが1なら、`true`を返す。
0なら`false`を返す。
`MotorDirection`型も引数に対応する。

#### 5.2.6 getMotorSpeed系

2,3バイト目の値を返す。
返り値は`uint8_t`。

#### 5.2.7 getCheckDigits

1バイト目を3ビット右シフトして、それと`0b1111`との論理積を返す。
右シフトでは上位のビットは0になるためたぶん論理積を出す必要はない。
