#include "Arduino.h"
#include "LED.h"

LED::LED(Adafruit_NeoPixel &neopixel) : neopixel(neopixel) {}
void LED::setup_neopixel(int num_LED)
{
    this->num_led = num_LED;
    // this->neopixel.begin();
}
double LED::motorLR2Hue(double left, double right)
{
    return (hue_is_in_left(left, right) ? left_hue(left, right) : right_hue(left, right));
}
double LED::motorLR2Value(double left, double right)
{
    return max(abs(left), abs(right)) * 100;
}
void LED::set_all_LED(double left, double right)
{
    RGB rgb = convert_from_HSV(motorLR2Hue(left, right), 100, motorLR2Value(left, right));
    this->neopixel.fill(Adafruit_NeoPixel::Color(rgb.G * 255, rgb.B * 255, rgb.R * 255), 0, num_led);
    this->neopixel.show();
}
double LED::left_hue(double left, double right)
{
    return -45 * left_add_right_normalize(left, right) + 90;
}
double LED::right_hue(double left, double right)
{
    return 45 * left_add_right_normalize(left, right) + 270;
}
double LED::left_add_right_normalize(double left, double right)
{
    if (max(abs(left), abs(right)) <= 0)
        return 0.;
    return (left + right) / max(abs(left), abs(right));
}
bool LED::hue_is_in_left(double left, double right)
{
    return (left - right <= 0);
}RGB LED::convert_from_HSV(double H, double S, double V)
{
    if (H > 360 || H < 0 || S > 100 || S < 0 || V > 100 || V < 0)
    {
        // Serial.println("The givem HSV values are not in valid range");
        return RGB(0, 0, 0);
    }
    double s = S / 100;
    double v = V / 100;
    double C = s * v;
    double X = C * (1 - abs(fmod(H / 60.0, 2) - 1));
    double m = v - C;
    double r, g, b;
    if (H >= 0 && H < 60)
    {
        r = C, g = X, b = 0;
    }
    else if (H >= 60 && H < 120)
    {
        r = X, g = C, b = 0;
    }
    else if (H >= 120 && H < 180)
    {
        r = 0, g = C, b = X;
    }
    else if (H >= 180 && H < 240)
    {
        r = 0, g = X, b = C;
    }
    else if (H >= 240 && H < 300)
    {
        r = X, g = 0, b = C;
    }
    else
    {
        r = C, g = 0, b = X;
    }
    return RGB(r, g, b);
}