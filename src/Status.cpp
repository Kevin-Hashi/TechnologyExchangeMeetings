#include <Arduino.h>
#include "Status.h"

Status::Status(int pin){
    this->pin=pin;
}
bool Status::isLow(){
    return this->pin_status==LOW;
}
bool Status::isHigh(){
    return this->pin_status==HIGH;
}
void Status::setStatus(){
    this->pin_status=digitalRead(this->pin);
}