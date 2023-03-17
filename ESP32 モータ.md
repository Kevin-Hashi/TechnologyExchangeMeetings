# ESP32 モータ

## 1 はじめに

### 1.1 この文書について

2023/03/18に行われた四校技術交換会において、浦和高校の使用した技術の一部を公開、説明するものである。

## 2 目的

ESP32でモータを動作させる。

## 3 利用した技術

### 3.1 ハードウェア

- ESP32
- モータ
- DRV8835
- ESP32の動作に必要なもの
- DRV8835モータドライバ <https://akizukidenshi.com/catalog/g/gK-09848>
- プログラム書き込みに必要なもの
  - PC
  - ケーブル
  - そのほか必要なもの

### 3.2 ソフトウェア

- Arduino IDE
- Visual Studio Code

## 4 手法

DRV8835を用いてPWMで速度を決定する。
SN754410を使用する場合は別の方法が必要。後述する。
また、開発時に使用しなかったため、`digitalWrite()`によるオンオフ動作を実装していない。初期化のみ実装している。

## 5 実装

### 5.1 ソースコード

#### 5.1.1 Pin.h

```cpp
#include "Arduino.h"
#ifndef INCLUDED_ESPPin_
#define INCLUDED_ESPPin_
class ESPPin{
    public:
    int pin;
    int channel;
    void setPin(int pin);
    void attach_channel(int channel);
};
#endif
```

#### 5.1.2 Pin.cpp

```cpp
#include "Pin.h"
void ESPPin::setPin(int pin){
    this->pin=pin;
}
void ESPPin::attach_channel(int channel){
    this->channel=channel;
    ledcAttachPin(this->pin, this->channel);
}
```

#### 5.1.3 motor.h

```cpp
#include "Arduino.h"
#include "Pin.h"

#ifndef INCLUDED_motor_
#define INCLUDED_motor_
enum class SetupType: int{
    Bin, PWM, notSetuped
};
class Motor{
    public:
    ESPPin pin1;
    ESPPin pin2;
    Motor(int pin1, int pin2);
    int setup();
    int setupForPWM(int channel1, int channel2, int ledc_base_freq, int ledc_timer_bit);
    int rotateByPWM(double pwm1, double pwm2);
    private:
    SetupType setupType;
};
#endif
```

#### 5.1.4 motor.cpp

```cpp
#include "Arduino.h"
#include "motor.h"

Motor::Motor(int pin_1, int pin_2){
    this->setupType=SetupType::notSetuped;
    this->pin1=ESPPin();
    this->pin2=ESPPin();
    this->pin1.setPin(pin_1);
    this->pin2.setPin(pin_2);
}
int Motor::setup(){
    if(this->setupType==SetupType::notSetuped){
        pinMode(this->pin1.pin, OUTPUT);
        pinMode(this->pin2.pin, OUTPUT);
        this->setupType=SetupType::Bin;
        return 0;
    }else{
        return 1;
    }
}
int Motor::setupForPWM(int channel1, int channel2, int ledc_base_freq, int ledc_timer_bit){
    if(this->setupType==SetupType::notSetuped){
        ledcSetup(channel1, ledc_base_freq, ledc_timer_bit);
        ledcSetup(channel2, ledc_base_freq, ledc_timer_bit);
        this->pin1.attach_channel(channel1);
        this->pin2.attach_channel(channel2);
        this->setupType=SetupType::PWM;
        return 0;
    }else{
        return 1;
    }
}
int Motor::rotateByPWM(double pwm1, double pwm2){
    if(this->setupType!=SetupType::PWM){
        return 1;
    }
    ledcWrite(this->pin1.channel, pwm1);
    ledcWrite(this->pin2.channel, pwm2);
    return 0;
}
```

### 5.2 解説

#### 5.2.1 ESPPin

##### 5.2.1.1 setPin

`ESPPin::pin`にピン番号を設定する。

##### 5.2.1.2 attach_channel

`ESPPin::channel`にチャンネル番号を設定する。
`ledcAttachPin`を使用してチャンネルに割り当てる。

#### 5.2.2 Motor

##### 5.2.2.1 constructor

- `ESPPin::setupType`を`SetupType::notSetuped`にする。
- pin1,pin2に引数のピン番号を割り当てる。

##### 5.2.2.2 setup

- `this->setupType==SetupType::notSetuped`ならば動作する。
  - pin1,pin2を`pinMode`でOUTPUTに設定する。
  - `ESPPin::setupType`を`SetupType::Bin`に設定する。
  - 0を返す。
- else
  - 1を返す。

##### 5.2.2.3 setupForPWM

- `this->setupType==SetupType::notSetuped`ならば動作する。
  - `ledcSetup`で引数のチャンネル、周波数、ビットを設定する。
  - pin1,pin2をチャンネルに割り当てる。
  - 0を返す。
- else
  - 1を返す。

## 6 補足

### 6.1 SN754410を駆動させる場合

このプログラムはDRV8835を使用することを想定しているので、制御するピンに直接PWMする。

そこでSN754410でPWMする方法について調べてた。
すると、

- 1A NOR 2AにPWMする派閥(<https://www.petitmonte.com/robot/howto_dc_motor_sn754410ne.html>)
- ENにPWMする派閥(<https://www.petitmonte.com/robot/howto_dc_motor_sn754410ne.html>)

の2種類があった。

個人的見解としては

- 前者は、1Aと2AがLの時に1Yと2YがLとなるのでブレーキ
- 後者は、ENがLの時に1Yと2YがHiZとなるので空転(電気的切り離し)

となるのではないかと予想する。

[データシート](https://www.ti.com/jp/lit/ds/symlink/sn754410.pdf?ts=1679071869987&ref_url=https%253A%252F%252Fwww.ti.com%252Fproduct%252Fja-jp%252FSN754410)の7.5によると、スイッチング特性は次の通りである

| PARAMETER  | DESCRIPTION                                                       |  MIN  |  TYP  |  MAX  | UNIT  |
| :--------- | :---------------------------------------------------------------- | :---: | :---: | :---: | :---: |
| $t_{d1}$   | Delay time, high-to-low-level output $t_{d1}$ 400 ns from A input |   >   |   >   |  400  |  ns   |
| $t_{d2}$   | Delay time, low-to-high-level output $t_{d2}$ 800 ns from A input |   >   |   >   |  800  |   ^   |
| $t_{TLH}$  | Transition time, low-to-high-level $t_{TLH}$ 300 ns output        |   >   |   >   |  300  |   ^   |
| $t_{THL}$  | Transition time, high-to-low-level $t_{THL}$ 300 ns output        |   >   |   >   |   ^   |   ^   |
| $t_{en1}$  | Enable time to the high level                                     |   >   |   >   |  700  |   ^   |
| $t_{en2}$  | Enable time to the low level                                      |   >   |   >   |  400  |   ^   |
| $t_{dis1}$ | Disable time from the high level                                  |   >   |   >   |  900  |   ^   |
| $t_{dis2}$ | Disable time from the low level                                   |   >   |   >   |  600  |   ^   |

となると、見た感じ応答性の良さはインプットをPWMするほうだと思う。

しかし、PWMを15.625kHz,8bitとすると、1周期64us(=64000ns)となる。

| Duty比 | on時間 | off時間 | UNIT  | $>t_{d1}$ | $>t_{d2}$ | $>t_{TLH}$ | $>t_{THL}$ | $>t_{en1}$ | $>t_{en2}$ | $>t_{dis1}$ | $>t_{dis2}$ |
| ------ | ------ | ------- | :---: | :-------: | :-------: | :--------: | :--------: | :--------: | :--------: | :---------: | :---------: |
| 0.5%   | 320    | 63680   |  ns   |     ×     |     ○     |     ○      |     ○      |     ×      |     ○      |      ×      |      ○      |
| 1%     | 640    | 63360   |   ^   |     ^     |     ^     |     ^      |     ^      |     ^      |     ^      |      ^      |      ^      |
| 1.5%   | 960    | 63040   |   ^   |     ○     |     ^     |     ^      |     ^      |     ○      |     ^      |      ○      |      ^      |
| 2%     | 1280   | 62720   |   ^   |     ^     |     ^     |     ^      |     ^      |     ^      |     ^      |      ^      |      ^      |
| 98%    | 62720  | 1280    |   ^   |     ^     |     ^     |     ^      |     ^      |     ^      |     ^      |      ^      |      ^      |
| 98.5%  | 63040  | 960     |   ^   |     ^     |     ^     |     ^      |     ^      |     ^      |     ^      |      ^      |      ^      |
| 99%    | 63360  | 640     |   ^   |     ^     |     ^     |     ^      |     ^      |     ^      |     ^      |      ^      |      ^      |
| 99.5%  | 63680  | 320     |   ^   |     ^     |     ×     |     ^      |     ^      |     ^      |     ×      |      ^      |      ×      |

この表が当てになるとすると、両方式のどちらを採用してもよい気がする。
どちらも2%～98.5%(限界に挑戦するなら1.5%～99%)、100%で使用できるはずである。
実際には少し違うので参考程度に。何が起こっても責任は負わない。
