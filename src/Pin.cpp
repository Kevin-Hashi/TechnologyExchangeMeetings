#include "Pin.h"
void ESPPin::setPin(int pin){
    this->pin=pin;
}
void ESPPin::attach_channel(int channel){
    this->channel=channel;
    ledcAttachPin(this->pin, this->channel);
}
