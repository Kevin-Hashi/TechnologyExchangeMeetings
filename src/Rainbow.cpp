#include "Arduino.h"
#include "LED.h"
#include "Rainbow.h"
#include "RGB.hpp"

Rainbow::Rainbow(Adafruit_NeoPixel &neopixel) : neopixel(neopixel) {}
int Rainbow::circle_degree = 360;
int Rainbow::setup_neopixel()
{
    this->neopixel.begin();
    this->clear();
}
int Rainbow::setup_ppp(int piece_per_lap)
{
    this->piece_per_lap = piece_per_lap;
    calc_one_led_degree(this->piece_per_lap);
}
int Rainbow::setup_num_LED(int num_LED)
{
    this->num_LED = num_LED;
    Serial.print("num_LED:");
    Serial.println(num_LED);
}
int Rainbow::setup_round_ms(int round_ms)
{
    Serial.print("round_ms:");
    Serial.println(round_ms);
    this->round_ms = round_ms;
}
int Rainbow::setup_term_ms(int term_ms)
{
    Serial.print("term_ms:");
    Serial.println(term_ms);
    this->term_ms = term_ms;
}
int Rainbow::rainbow_LED()
{
    unsigned long start_ms = millis();
    unsigned long end_ms = millis();
    unsigned long last_back_to_0 = millis();
    Serial.print("start_ms:");
    Serial.println(start_ms);
    Serial.print("end_ms:");
    Serial.println(end_ms);
    Serial.print("last_back_to_0:");
    Serial.println(last_back_to_0);
    int cnt = 0;
    while (end_ms - start_ms < this->term_ms)
    {
        unsigned long now_millis = millis();
        Serial.print("now_millis:");
        Serial.println(now_millis);
        double add_hue = (double)(now_millis - last_back_to_0) / this->round_ms * this->circle_degree;
        Serial.print("now_millis-last_back_to_0:");
        Serial.println((now_millis - last_back_to_0));
        Serial.print("add_hue:");
        Serial.println(add_hue);
        for (int i = 0; i < this->num_LED; i++)
        {
            RGB rgb = LED::convert_from_HSV(fmod(this->calc_hue(i) + add_hue, (double)this->circle_degree), 100, 100);
            this->neopixel.setPixelColor(i, Adafruit_NeoPixel::Color(rgb.G * 255, rgb.B * 255, rgb.R * 255));
        }
        this->neopixel.show();
        Serial.print("(now_millis - last_back_to_0) >= this->round_ms:");
        Serial.println((now_millis - last_back_to_0) >= this->round_ms);
        if ((now_millis - last_back_to_0) >= this->round_ms)
        {
            last_back_to_0 = millis();
        }
        end_ms = millis();
    }
}
int Rainbow::clear(){
    this->neopixel.clear();
    this->neopixel.show();
}
double Rainbow::calc_hue(double n)
{
    int modn = fmod(n, (double)this->num_LED);
    return this->one_led_degree * modn;
}
void Rainbow::calc_one_led_degree(int n)
{
    this->one_led_degree = this->circle_degree / (double)n;
}