#ifndef INCLUDED_MotorDirection_
#define INCLUDED_MotorDirection_
enum class MotorDirection : bool
{
    Forward,
    Back
};
#endif
#ifndef INCLUDED_MotorByte_
#define INCLUDED_MotorByte_
class MotorByte
{
public:
    static constexpr int array_size = 3;
    uint8_t array[array_size];
    MotorByte(void);
    void setArray(uint8_t array[]);
    void setLED(bool led);
    bool getLED();
    void setLeftMotorDirection(bool back);
    void setLeftMotorDirection(MotorDirection direction);
    MotorDirection getLeftMotorDirection();
    void setRightMotorDirection(bool back);
    void setRightMotorDirection(MotorDirection direction);
    MotorDirection getRightMotorDirection();
    void setLeftMotorSpeed(uint8_t speed);
    uint8_t getLeftMotorSpeed();
    void setRightMotorSpeed(uint8_t speed);
    uint8_t getRightMotorSpeed();
    uint8_t calcCheckDigits(uint8_t first_byte);
    void setCheckDigits();
    uint8_t getCheckDigits();
};
#endif