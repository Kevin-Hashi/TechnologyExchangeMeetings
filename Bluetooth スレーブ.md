# Bluetooth スレーブ側

## 1 はじめに

### 1.1 この文書について

2023/03/18に行われた四校技術交換会において、浦和高校の使用した技術の一部を公開、説明するものである。

## 2 目的

Bluetoothを使用して、ESP32またはAndroidをマスター、ESP32をスレーブとしてロボットを動作させる。

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

### 3.3 ライブラリ

- Arduino-ESP32 (<https://github.com/espressif/arduino-esp32>)
- Adafruit_NeoPixel (<https://github.com/adafruit/Adafruit_NeoPixel>)
- motor (「[ESP32モータ](ESP32%20モータ.md)」を参照)
- MotorByte (「[Bluetoothバイト通信](Bluetooth%20バイト通信.md)」を参照)

## 4 手法

次のような流れに沿って動作させる

1. マスターが制御用のバイト列を送信する
2. スレーブが受け取る
3. バイトを解釈して、モーター等の制御信号に変換する

### 4.1 必要な技術

- ビット演算
- チェックデジットの計算

## 5 実装

使用したソースコードの一部を抜粋して掲載する。

### 5.1 ソースコード

#### 5.1.1 ESP32_DDR_merged.ino

```cpp
#include "BluetoothSerial.h"
#include "Adafruit_NeoPixel.h"
#include "motor.h"
#include "LED.h"
#include "Rainbow.h"
#include "check.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

uint8_t receive_byte[MotorByte::array_size] = {0};
constexpr uint8_t zero_array[MotorByte::array_size] = {0};
MotorByte motorByte;

constexpr int num_LED = 43;
Adafruit_NeoPixel pixels(num_LED, 27, NEO_GBR + NEO_KHZ800);
LED led(pixels);
Rainbow rainbow(pixels);

TaskHandle_t thp[1];

bool led_lock = false;
void rainbow_task(void *args)
{
    led_lock = true;
    rainbow.rainbow_LED();
    clear_led();
    led_lock = false;
    vTaskDelete(NULL);
}

constexpr int rainbow_piece = 20;
constexpr int rainbow_round_ms = 2000;
constexpr int rainbow_term_ms = 5000;

constexpr int left1 = 32;
constexpr int left2 = 33;
constexpr int right1 = 25;
constexpr int right2 = 26;

constexpr int CHANNEL0 = 0;
constexpr int CHANNEL1 = 1;
constexpr int CHANNEL2 = 2;
constexpr int CHANNEL3 = 3;

constexpr int LEDC_TIMER_BIT = 8;
constexpr int LEDC_BASE_FREQ = 500;
constexpr int VALUE_MAX = 255;

constexpr int serial_speed = 9600;

Motor left(left1, left2), right(right1, right2);

bool not_setuped = true;

std::pair<int, int> rotate(uint8_t left_speed, uint8_t right_speed, MotorDirection left_back, MotorDirection right_back)
{
    int error_left = 0, error_right = 0;
    if (left_back == MotorDirection::Forward)
    {
        error_left = left.rotateByPWM(left_speed, 0);
    }
    else
    {
        error_left = left.rotateByPWM(0, left_speed);
    }
    if (right_back == MotorDirection::Forward)
    {
        error_right = right.rotateByPWM(right_speed, 0);
    }
    else
    {
        error_right = right.rotateByPWM(0, right_speed);
    }
    return std::make_pair(error_left, error_right);
}

void setup()
{
    Serial.begin(serial_speed);
    clear_led();
    led.setup_neopixel(num_LED);
    rainbow.setup_neopixel();
    rainbow.setup_ppp(rainbow_piece);
    rainbow.setup_num_LED(num_LED);
    rainbow.setup_round_ms(rainbow_round_ms);
    rainbow.setup_term_ms(rainbow_term_ms);
    SerialBT.begin("ESP32test");
    Serial.println("The device started, now you can pair it with bluetooth!");
    uint8_t mac_bt[6];
    esp_read_mac(mac_bt, ESP_MAC_BT);
    Serial.printf("[Bluetooth] Mac Address = %02X:%02X:%02X:%02X:%02X:%02X\r\n", mac_bt[0], mac_bt[1], mac_bt[2], mac_bt[3], mac_bt[4], mac_bt[5]);
}

void loop()
{
    if (not_setuped)
    {
        int error_left = left.setupForPWM(CHANNEL0, CHANNEL1, LEDC_BASE_FREQ, LEDC_TIMER_BIT);
        int error_right = right.setupForPWM(CHANNEL2, CHANNEL3, LEDC_BASE_FREQ, LEDC_TIMER_BIT);
        if (error_left)
        {
            Serial.println("Left motor setup failed!");
        }
        if (error_right)
        {
            Serial.println("Right motor setup failed!");
        }
        not_setuped = false;
    }
    if (SerialBT.available())
    {
        uint8_t temp_byte[MotorByte::array_size] = {0};
        for (int i = 0; i < MotorByte::array_size; i++)
        {
            temp_byte[i] = SerialBT.read();
        }
        if (temp_byte != zero_array)
        {
            for (int i = 0; i < MotorByte::array_size; i++)
            {
                Serial.print(temp_byte[i]);
                Serial.print(' ');
            }
            for (int i = 0; i < MotorByte::array_size; i++)
            {
                receive_byte[i] = temp_byte[i];
            }
            Serial.println();
            motorByte.setArray(receive_byte);
            bool rainbowLED = motorByte.getLED();
            MotorDirection leftMotorDirection = motorByte.getLeftMotorDirection();
            MotorDirection rightMotorDirection = motorByte.getRightMotorDirection();
            uint8_t leftSpeed = motorByte.getLeftMotorSpeed();
            uint8_t rightSpeed = motorByte.getRightMotorSpeed();
            uint8_t digits = motorByte.getCheckDigits();
            uint8_t calc_digits = motorByte.calcCheckDigits(motorByte.array[0] & 0b00000111);
            if (digits == calc_digits)
            {
                if (!not_setuped)
                {
                    std::pair<int, int> motor_error = rotate(leftSpeed, rightSpeed, leftMotorDirection, rightMotorDirection);
                    if (motor_error.first)
                    {
                        Serial.println("Left motor rotation failed!");
                    }
                    if (motor_error.second)
                    {
                        Serial.println("Right motor rotation failed!");
                    }
                    if (!led_lock && rainbowLED)
                    {
                        xTaskCreatePinnedToCore(rainbow_task, "rainbow_task", 4096, NULL, 1, &thp[0], 0);
                    }
                    double left_p = leftSpeed / VALUE_MAX*((static_cast<bool>(leftMotorDirection))?-1:1), right_p = rightSpeed / VALUE_MAX*((static_cast<bool>(rightMotorDirection))?-1:1);
                    Serial.print(left_p);
                    Serial.print(" ");
                    Serial.println(right_p);
                    Serial.println(led.motorLR2Hue(left_p, right_p));
                    //Serial.println(led.motorLR2Value(left_p, right_p));
                    if (!led_lock)
                    {
                        led.set_all_LED(left_p, right_p);
                    }
                }
            }
        }
    }
    delay(20);
}

void clear_led()
{
    pixels.clear();
    pixels.show();
}
```

### 5.2 解説

#### 5.2.1 #include

##### 5.2.1.1 BluetoothSerial

Arduino-ESP32に含まれているライブラリを使用する。
インストール方法は各自確認してほしい。
<https://www.mgo-tec.com/arduino-core-esp32-install>

##### 5.2.1.2 Adafruit_NeoPixel

サンプルはこれ
<https://wak-tech.com/archives/1574>

##### 5.2.1.3 motor

「[ESP32モータ](ESP32%20モータ.md)」を参照してほしい

##### 5.2.1.4 LED

不要なので解説しない。必要なら頑張ってソースを読んでほしい。

##### 5.2.1.5 Rainbow

不要なので解説しない。必要なら頑張ってソースを読んでほしい。

##### 5.2.1.6 check

「[Bluetoothバイト通信](Bluetooth%20バイト通信.md)」を参照してほしい

#### 5.2.2 #if

もともとのソースコードには書いてあった。
必要なのかはわからないがとりあえず書いている。

#### 5.2.3 グローバル変数

説明が難しいのでパスする。

`not_setuped`は`left.setupType == SetupType.notSetuped && right.setupType == SetupType.notSetuped`で代用すればよいはず。必要だがムダなので修正してほしい。

#### 5.2.4 rainbow_task

非同期で虹色に光らせる。
終了したらタスクを自動的に削除する。
同時に1つしか起動しない。

#### 5.2.5 rotate

モータを回転させる。
引数として、回転量`uint8_t`、向き`MotorDirection`を左右それぞれとる。
返り値は`pair<int, int>`で、デフォルトは`[0, 0]`。エラーのあるほうが1になる。

#### 5.2.6 setup

- `Serial`の初期化
- LEDを消灯
- 虹色のやつの初期化
- `BluetoothSerial`の初期化
- ESP32のBluetoothのMACアドレスを取得

#### 5.2.7 loop

- 1周目の動作
  - 左右のモータをPWMモードで初期化する。
  - [5.2.3](#523-グローバル変数)の`not_setuped`についての記述を見ておいてほしい。
- 共通の動作
  - `SerialBT.available()`が0ではないときに動作する。早期リターンにしたほうが見やすい。
    - temp_byteに読み込んだバイト列を移す。
    - 読み込んだバイトが、3つとも0ではないならば次の動作をする。早期リターンにしたほうが見やすい。
      - `motorByte`にバイト列をセットする。
      - LEDを光らせるか、モータの回転向き、回転量、チェックデジットを取得する。
      - チェックデジットを計算する。
      - バイト列のチェックデジットと計算したチェックデジットが同じか判定する。早期リターンにしたほうが見やすい。
        - `!not_setuped`の時に動作する。早期リターンにしたほうが見やすい。
          - モーターを回転させる。[5.2.5 rotate](#525-rotate)を参照。
          - 回転時にエラーがないかを判定。あったらシリアルに書き出す。
          - `rainbowLED == true`かつロックがされていないならばLEDを光らせる。
          - ロックされていないとき（虹色に光っていないときに）向きに合わせた色で光らせる。
  - 20ms休止する。
