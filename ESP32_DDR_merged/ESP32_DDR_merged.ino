#include "BluetoothSerial.h"
#include "Adafruit_NeoPixel.h"
#include "motor.h"
#include "LED.h"
#include "Rainbow.h"
#include "check.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

uint8_t receive_byte[MotorByte::array_size] = {0};
constexpr uint8_t zero_array[MotorByte::array_size] = {0};
MotorByte motorByte;

constexpr int num_LED = 43;
Adafruit_NeoPixel pixels(num_LED, 27, NEO_GBR + NEO_KHZ800);
LED led(pixels);
Rainbow rainbow(pixels);

TaskHandle_t thp[1];

bool led_lock = false;
void rainbow_task(void *args)
{
    led_lock = true;
    rainbow.rainbow_LED();
    clear_led();
    led_lock = false;
    vTaskDelete(NULL);
}

constexpr int rainbow_piece = 20;
constexpr int rainbow_round_ms = 2000;
constexpr int rainbow_term_ms = 5000;

constexpr int left1 = 32;
constexpr int left2 = 33;
constexpr int right1 = 25;
constexpr int right2 = 26;

constexpr int CHANNEL0 = 0;
constexpr int CHANNEL1 = 1;
constexpr int CHANNEL2 = 2;
constexpr int CHANNEL3 = 3;

constexpr int LEDC_TIMER_BIT = 8;
constexpr int LEDC_BASE_FREQ = 500;
constexpr int VALUE_MAX = 255;

constexpr int serial_speed = 9600;

Motor left(left1, left2), right(right1, right2);

bool not_setuped = true;

std::pair<int, int> rotate(uint8_t left_speed, uint8_t right_speed, MotorDirection left_back, MotorDirection right_back)
{
    int error_left = 0, error_right = 0;
    if (left_back == MotorDirection::Forward)
    {
        error_left = left.rotateByPWM(left_speed, 0);
    }
    else
    {
        error_left = left.rotateByPWM(0, left_speed);
    }
    if (right_back == MotorDirection::Forward)
    {
        error_right = right.rotateByPWM(right_speed, 0);
    }
    else
    {
        error_right = right.rotateByPWM(0, right_speed);
    }
    return std::make_pair(error_left, error_right);
}

void setup()
{
    Serial.begin(serial_speed);
    clear_led();
    led.setup_neopixel(num_LED);
    rainbow.setup_neopixel();
    rainbow.setup_ppp(rainbow_piece);
    rainbow.setup_num_LED(num_LED);
    rainbow.setup_round_ms(rainbow_round_ms);
    rainbow.setup_term_ms(rainbow_term_ms);
    SerialBT.begin("ESP32test");
    Serial.println("The device started, now you can pair it with bluetooth!");
    uint8_t mac_bt[6];
    esp_read_mac(mac_bt, ESP_MAC_BT);
    Serial.printf("[Bluetooth] Mac Address = %02X:%02X:%02X:%02X:%02X:%02X\r\n", mac_bt[0], mac_bt[1], mac_bt[2], mac_bt[3], mac_bt[4], mac_bt[5]);
}

void loop()
{
    if (not_setuped)
    {
        int error_left = left.setupForPWM(CHANNEL0, CHANNEL1, LEDC_BASE_FREQ, LEDC_TIMER_BIT);
        int error_right = right.setupForPWM(CHANNEL2, CHANNEL3, LEDC_BASE_FREQ, LEDC_TIMER_BIT);
        if (error_left)
        {
            Serial.println("Left motor setup failed!");
        }
        if (error_right)
        {
            Serial.println("Right motor setup failed!");
        }
        not_setuped = false;
    }
    if (SerialBT.available())
    {
        uint8_t temp_byte[MotorByte::array_size] = {0};
        for (int i = 0; i < MotorByte::array_size; i++)
        {
            temp_byte[i] = SerialBT.read();
        }
        if (temp_byte != zero_array)
        {
            for (int i = 0; i < MotorByte::array_size; i++)
            {
                Serial.print(temp_byte[i]);
                Serial.print(' ');
            }
            for (int i = 0; i < MotorByte::array_size; i++)
            {
                receive_byte[i] = temp_byte[i];
            }
            Serial.println();
            motorByte.setArray(receive_byte);
            bool rainbowLED = motorByte.getLED();
            MotorDirection leftMotorDirection = motorByte.getLeftMotorDirection();
            MotorDirection rightMotorDirection = motorByte.getRightMotorDirection();
            uint8_t leftSpeed = motorByte.getLeftMotorSpeed();
            uint8_t rightSpeed = motorByte.getRightMotorSpeed();
            uint8_t digits = motorByte.getCheckDigits();
            uint8_t calc_digits = motorByte.calcCheckDigits(motorByte.array[0] & 0b00000111);
            if (digits == calc_digits)
            {
                if (!not_setuped)
                {
                    std::pair<int, int> motor_error = rotate(leftSpeed, rightSpeed, leftMotorDirection, rightMotorDirection);
                    if (motor_error.first)
                    {
                        Serial.println("Left motor rotation failed!");
                    }
                    if (motor_error.second)
                    {
                        Serial.println("Right motor rotation failed!");
                    }
                    if (!led_lock && rainbowLED)
                    {
                        xTaskCreatePinnedToCore(rainbow_task, "rainbow_task", 4096, NULL, 1, &thp[0], 0);
                    }
                    double left_p = leftSpeed / VALUE_MAX*((static_cast<bool>(leftMotorDirection))?-1:1), right_p = rightSpeed / VALUE_MAX*((static_cast<bool>(rightMotorDirection))?-1:1);
                    Serial.print(left_p);
                    Serial.print(" ");
                    Serial.println(right_p);
                    Serial.println(led.motorLR2Hue(left_p, right_p));
                    //Serial.println(led.motorLR2Value(left_p, right_p));
                    if (!led_lock)
                    {
                        led.set_all_LED(left_p, right_p);
                    }
                }
            }
        }
    }
    delay(20);
}

void clear_led()
{
    pixels.clear();
    pixels.show();
}