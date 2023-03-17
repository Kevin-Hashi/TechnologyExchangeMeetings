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