#ifndef INCLUDED_RGB_
#define INCLUDED_RGB_

class RGB
{
public:
    RGB(double R, double G, double B)
    {
        this->R = R;
        this->G = G;
        this->B = B;
    }
    double R;
    double G;
    double B;
};
#endif