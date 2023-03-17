#include <Arduino.h>
#ifndef INCLUDED_Status_
#define INCLUDED_Status_
class Status {
    public:
    int pin;
    int pin_status;
    Status(int pin);
    bool isLow();
    bool isHigh();
    void setStatus();
};
#endif