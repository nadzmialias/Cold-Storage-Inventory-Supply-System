# CSISS ESP32 Temperature Node (`iot/csiss_esp32/`)

Reads live cold-storage temperature from a DS18B20 probe, displays status on a 16x2 I2C LCD, uploads readings to the `ingest-temperature` Edge Function, and subscribes to Supabase Realtime broadcasts so admin threshold updates sync immediately.

## Hardware Pinout

| Peripheral | ESP32 Pin | Notes |
| :--- | :--- | :--- |
| DS18B20 Data (`ONE_WIRE_BUS`) | GPIO 4 | Requires 4.7 kOhm pull-up resistor to 3.3 V |
| 16x2 LCD I2C SDA | GPIO 21 | I2C address `0x27` |
| 16x2 LCD I2C SCL | GPIO 22 | I2C address `0x27` |

## Required Arduino Libraries
- `OneWire`
- `DallasTemperature`
- `LiquidCrystal_I2C`
- `WebSockets` (`WebSocketsClient`)
- ESP32 Board Package (`WiFi`, `WiFiClientSecure`, `HTTPClient`)

## Setup & Flashing
1. Copy `iot/csiss_esp32/secrets.example.h` to `iot/csiss_esp32/secrets.h` (gitignored).
2. Fill in `WIFI_SSID`, `WIFI_PASSWORD`, `IOT_DEVICE_KEY`, and `SUPABASE_PUBLISHABLE_KEY`.
3. Open `iot/csiss_esp32/csiss_esp32.ino` in Arduino IDE, select your ESP32 board and COM port, and upload.

> **Note:** `csiss_esp32.ino` currently uses `secureClient.setInsecure()` for prototype TLS connections; pin a root CA certificate before production deployment.
