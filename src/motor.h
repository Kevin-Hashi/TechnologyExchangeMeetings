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