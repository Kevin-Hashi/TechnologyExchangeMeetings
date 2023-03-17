#include "Arduino.h"
#include "Adafruit_NeoPixel.h"
#include "RGB.hpp"

#ifndef INCLUDED_LED_
#define INCLUDED_LED_
class LED
{
public:
    LED(Adafruit_NeoPixel &neopixel);
    Adafruit_NeoPixel &neopixel;
    int num_led;
    void setup_neopixel(int num_LED);
    double motorLR2Hue(double left, double right);
    double motorLR2Value(double left, double right);
    void set_all_LED(double left, double right);
    static RGB convert_from_HSV(double H, double S, double V);

private:
    bool hue_is_in_left(double left, double right);
    double left_hue(double left, double right);
    double right_hue(double left, double right);
    double left_add_right_normalize(double left, double right);
};
#endif