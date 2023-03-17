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