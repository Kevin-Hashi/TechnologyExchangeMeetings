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
    uint8_t digited_first_byte = this->calcCheckDigits(first_byte);
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