# Bluetooth マスター側

## 1 はじめに

### 1.1 この文書について

2023/03/18に行われた四校技術交換会において、浦和高校の使用した技術の一部を公開、説明するものである。

## 2 目的

Bluetoothを使用して、ESP32またはAndroidをマスター、ESP32をスレーブとしてロボットを動作させる。

## 3 利用した技術

### 3.1 ハードウェア

- ESP32
- ESP32の動作に必要なもの
- スレーブのESP32 (「[Bluetoothスレーブ](Bluetooth%20スレーブ.md)」を参照)
- プログラム書き込みに必要なもの
  - PC
  - ケーブル
  - そのほか必要なもの

### 3.2 ソフトウェア

- Arduino IDE
- Visual Studio Code

### 3.3 ライブラリ

- Arduino-ESP32 (<https://github.com/espressif/arduino-esp32>)
- motor (「[ESP32モータ](ESP32%20モータ.md)」を参照)
- MotorByte (「[Bluetoothバイト通信](Bluetooth%20バイト通信.md)」を参照)

## 4 手法

設定した前後左右の向きのピンとLEDのピンから入力を取得する。
バイト列に変換する。
スレーブに送信する。

## 5 実装

### 5.1 ソースコード

#### 5.1.1 SerialToSerialBTM.ino

```cpp
#include "BluetoothSerial.h"
#include "Status.h"
#include "motor.h"
#include "check.h"

BluetoothSerial SerialBT;

uint8_t address[6] = {0x3C, 0x61, 0x05, 0x13, 0xE3, 0xC2};
// uint8_t address[6]  = {0x00, 0x1D, 0xA5, 0x02, 0xC3, 0x22};
char *pin = "1234"; //<- standard pin would be provided by default
bool connected;

Status forward(26), left(25), back(33), right(32), rainbow(27);
double last_left = 0., last_right = 0.;
bool last_rainbow = false;
MotorByte motorByte;
constexpr int MAX_VALUE = 255;
int left_d = 0, right_d = 0, cnt = 0;

void inc_cnt()
{
  cnt += 1;
}
void set_stop()
{
  motorByte.setLeftMotorSpeed(0);
  motorByte.setLeftMotorDirection(false);
  motorByte.setRightMotorSpeed(0);
  motorByte.setRightMotorDirection(false);
  left_d = 0;
  right_d = 0;
  cnt = 0;
}
void add_forward()
{
  left_d += 1;
  right_d += 1;
  inc_cnt();
}
void add_left()
{
  left_d -= 1;
  right_d += 1;
  inc_cnt();
}
void add_back()
{
  left_d -= 1;
  right_d -= 1;
  inc_cnt();
}
void add_right()
{
  left_d += 1;
  right_d -= 1;
  inc_cnt();
}

void setup()
{
  Serial.begin(9600);

  pinMode(forward.pin, INPUT_PULLUP);
  pinMode(left.pin, INPUT_PULLUP);
  pinMode(back.pin, INPUT_PULLUP);
  pinMode(right.pin, INPUT_PULLUP);
  pinMode(rainbow.pin, INPUT_PULLUP);

  SerialBT.begin("ESP32test", true);
  Serial.println("The device started in master mode, make sure remote BT device is on!");

  connected = SerialBT.connect(address);

  if (connected)
  {
    Serial.println("Connected Succesfully!");
  }
  else
  {
    while (!SerialBT.connected(250))
    {
      Serial.println("Failed to connect. Make sure remote device is available and in range, then restart app.");
    }
  }
}

void loop()
{
  forward.setStatus();
  left.setStatus();
  back.setStatus();
  right.setStatus();
  rainbow.setStatus();
  motorByte.setLED(rainbow.isLow());
  if (forward.isHigh() && left.isHigh() && back.isHigh() && right.isHigh())
  {
    set_stop();
  }
  else
  {
    if (forward.isLow())
    {
      add_forward();
    }
    if (left.isLow())
    {
      add_left();
    }
    if (back.isLow())
    {
      add_back();
    }
    if (right.isLow())
    {
      add_right();
    }
    motorByte.setLeftMotorSpeed(abs(left_d) * MAX_VALUE / cnt);
    motorByte.setLeftMotorDirection(left_d < 0);
    motorByte.setRightMotorSpeed(abs(right_d) * MAX_VALUE / cnt);
    motorByte.setRightMotorDirection(right_d < 0);
  }

  if (motorByte.getLeftMotorSpeed() != last_left || motorByte.getRightMotorSpeed() != last_right || motorByte.getLED() != last_rainbow)
  {
    motorByte.setCheckDigits();
    SerialBT.write(motorByte.array, motorByte.array_size);
  }
  last_left = motorByte.getLeftMotorSpeed();
  last_right = motorByte.getRightMotorSpeed();
  last_rainbow = motorByte.getLED();
  set_stop();
  delay(40);
}
```

### 5.2 解説

#### 5.2.1 #include

##### 5.2.1.1 BluetoothSerial

Arduino-ESP32に含まれているライブラリを使用する。
インストール方法は各自確認してほしい。
<https://www.mgo-tec.com/arduino-core-esp32-install>

##### 5.2.1.2 motor

「[ESP32モータ](ESP32%20モータ.md)」を参照してほしい

##### 5.2.1.3 check

「[Bluetoothバイト通信](Bluetooth%20バイト通信.md)」を参照してほしい

#### 5.2.2 グローバル変数

##### 5.2.2.1 address

スレーブのMACアドレス。「[Bluetoothスレーブ-setup](Bluetooth%20スレーブ.md#526-setup)」を参照。

##### 5.2.2.2 last_left,last_right

直前の回転量

##### 5.2.2.3 last_rainbow

直前に光らせたか

##### 5.2.2.4 left_d,right_d,cnt

左右それぞれの回転成分とベクトルの数

#### 5.2.2.5 set_stop

`motorByte`のsetSpeed系を0にし、setDirection系を`false`にする。
left_d,right_d,cntを0にする。

#### 5.2.2.6 add系

`cnt+=1`する。

##### 5.2.2.6.1 add_forward

left_d,right_dにそれぞれ+1する。

##### 5.2.2.6.2 add_left

left_dに-1、right_dに+1する。

##### 5.2.2.6.3 add_right

left_dに+1、right_dに-1する。

##### 5.2.2.6.4 add_back

left_d,right_dにそれぞれ-1する。

#### 5.2.2.7 setup

- `Serial`の初期化
- 各ピンの初期化
- `BluetoothSerial`の初期化
- 接続されるまで待つ

#### 5.2.2.8 loop

- 各ピンの状態を取得する。
- モータの向きのピンがすべてHなら(押していないなら)`set_stop`する。
- forwardがLなら(押されているなら)`add_forward`する。
- leftがLなら(押されているなら)`add_left`する。
- rightがLなら(押されているなら)`add_right`する。
- backがLなら(押されているなら)`add_back`する。
- 左右の回転量を正規化して`MAX_VALUE`まで拡大する。
- 左右の回転の向きを政府から判定する。
- 前とデータが違うなら送信する。
- last系に代入する。
- `set_stop`で初期化する。
- 40ms休止する。
