// This example code is in the Public Domain (or CC0 licensed, at your option.)
// By Victor Tchistiak - 2019
//
// This example demostrates master mode bluetooth connection and pin
// it creates a bridge between Serial and Classical Bluetooth (SPP)
// this is an extention of the SerialToSerialBT example by Evandro Copercini - 2018
//

#include "BluetoothSerial.h"
#include "Status.h"
#include "motor.h"
#include "check.h"

BluetoothSerial SerialBT;

String MACadd = "EC:62:60:84:0D:EE";
uint8_t address[6] = {0x3C, 0x61, 0x05, 0x13, 0xE3, 0xC2};
// uint8_t address[6]  = {0x00, 0x1D, 0xA5, 0x02, 0xC3, 0x22};
String name = "OBDII";
char *pin = "1234"; //<- standard pin would be provided by default
bool connected;

Status forward(26), left(25), back(33), right(32), rainbow(27);
double last_left = 0., last_right = 0.;
bool last_rainbow = false;
MotorByte motorByte;
constexpr int MAX_VALUE = 255;
int left_d = 0, right_d = 0, cnt = 0;

void inc_cnt()
{
  cnt += 1;
}
void set_stop()
{
  motorByte.setLeftMotorSpeed(0);
  motorByte.setLeftMotorDirection(false);
  motorByte.setRightMotorSpeed(0);
  motorByte.setRightMotorDirection(false);
  left_d = 0;
  right_d = 0;
  cnt = 0;
}
void add_forward()
{
  left_d += 1;
  right_d += 1;
  inc_cnt();
}
void add_left()
{
  left_d -= 1;
  right_d += 1;
  inc_cnt();
}
void add_back()
{
  left_d -= 1;
  right_d -= 1;
  inc_cnt();
}
void add_right()
{
  left_d += 1;
  right_d -= 1;
  inc_cnt();
}

void setup()
{
  Serial.begin(9600);

  pinMode(forward.pin, INPUT_PULLUP);
  pinMode(left.pin, INPUT_PULLUP);
  pinMode(back.pin, INPUT_PULLUP);
  pinMode(right.pin, INPUT_PULLUP);
  pinMode(rainbow.pin, INPUT_PULLUP);

  SerialBT.begin("ESP32test", true);
  Serial.println("The device started in master mode, make sure remote BT device is on!");

  connected = SerialBT.connect(address);

  if (connected)
  {
    Serial.println("Connected Succesfully!");
  }
  else
  {
    while (!SerialBT.connected(250))
    {
      Serial.println("Failed to connect. Make sure remote device is available and in range, then restart app.");
    }
  }
}

void loop()
{
  forward.setStatus();
  left.setStatus();
  back.setStatus();
  right.setStatus();
  rainbow.setStatus();
  motorByte.setLED(rainbow.isLow());
  if (forward.isHigh() && left.isHigh() && back.isHigh() && right.isHigh())
  {
    set_stop();
  }
  else
  {
    if (forward.isLow())
    {
      add_forward();
    }
    if (left.isLow())
    {
      add_left();
    }
    if (back.isLow())
    {
      add_back();
    }
    if (right.isLow())
    {
      add_right();
    }
    motorByte.setLeftMotorSpeed(abs(left_d) * MAX_VALUE / cnt);
    motorByte.setLeftMotorDirection(left_d < 0);
    motorByte.setRightMotorSpeed(abs(right_d) * MAX_VALUE / cnt);
    motorByte.setRightMotorDirection(right_d < 0);
  }

  if (motorByte.getLeftMotorSpeed() != last_left || motorByte.getRightMotorSpeed() != last_right || motorByte.getLED() != last_rainbow)
  {
    motorByte.setCheckDigits();
    SerialBT.write(motorByte.array, motorByte.array_size);
  }
  last_left = motorByte.getLeftMotorSpeed();
  last_right = motorByte.getRightMotorSpeed();
  last_rainbow = motorByte.getLED();
  set_stop();
  delay(40);
}
