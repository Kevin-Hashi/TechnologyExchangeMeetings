#include "Adafruit_NeoPixel.h"

#ifndef INCLUDED_Rainbow_
#define INCLUDED_Rainbow_

class Rainbow
{
public:
    Rainbow(Adafruit_NeoPixel &neopixel);
    int setup_neopixel();
    int setup_ppp(int piece_per_lap);
    int setup_num_LED(int num_LED);
    int setup_round_ms(int round_ms);
    int setup_term_ms(int term_ms);
    int rainbow_LED();
    int clear();

private:
    Adafruit_NeoPixel &neopixel;
    int piece_per_lap;
    int num_LED;
    int round_ms;
    int term_ms;
    double one_led_degree;
    static int circle_degree;
    double calc_hue(double n);
    void calc_one_led_degree(int n);
};
#endif