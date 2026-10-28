#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>

#include <Wire.h>
#include <LiquidCrystal_I2C.h>

#include <OneWire.h>
#include <DallasTemperature.h>

#include <math.h>

#include "secrets.h"

// ===================================
// SUPABASE CONFIGURATION
// ===================================

const char* SUPABASE_ENDPOINT =
  "https://zujynwdptdmnopfhbuxn.supabase.co"
  "/functions/v1/ingest-temperature";

// ===================================
// HARDWARE CONFIGURATION
// ===================================

#define ONE_WIRE_BUS 4

#define LCD_SDA 21
#define LCD_SCL 22

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);

LiquidCrystal_I2C lcd(0x27, 16, 2);

// ===================================
// TEMPERATURE LIMITS
// ===================================

const float MIN_TEMP = 20.0;
const float MAX_TEMP = 34.0;

// ===================================
// TIMING CONFIGURATION
// ===================================

const unsigned long SENSOR_INTERVAL = 2000;
const unsigned long UPLOAD_INTERVAL = 60000;
const unsigned long RETRY_INTERVAL = 10000;
const unsigned long WIFI_RETRY_INTERVAL = 15000;

// ===================================
// VARIABLES
// ===================================

float currentTemperature = NAN;

bool temperatureValid = false;
bool wasAbnormal = false;
bool urgentUpload = false;
bool hasUploaded = false;

unsigned long lastSensorRead = 0;
unsigned long lastSuccessfulUpload = 0;
unsigned long lastUploadAttempt = 0;
unsigned long lastWiFiRetry = 0;

// ===================================
// WIFI CONNECTION
// ===================================

void connectWiFi() {

  WiFi.mode(WIFI_STA);

  WiFi.begin(
    WIFI_SSID,
    WIFI_PASSWORD
  );

  Serial.println("Connecting to WiFi...");
}

// ===================================
// LCD UPDATE
// ===================================

void updateLCD() {

  lcd.setCursor(0, 0);

  if (temperatureValid) {

    lcd.print("Temp: ");
    lcd.print(currentTemperature, 2);
    lcd.print(" C   ");

  } else {

    lcd.print("Sensor Error    ");
  }

  lcd.setCursor(0, 1);

  if (WiFi.status() == WL_CONNECTED) {

    lcd.print("WiFi Connected  ");

  } else {

    lcd.print("WiFi Offline    ");
  }
}

// ===================================
// READ TEMPERATURE
// ===================================

void readTemperature() {

  sensors.requestTemperatures();

  float tempC = sensors.getTempCByIndex(0);

  // Reject disconnected and invalid readings.
  // 85 C can indicate an uninitialized DS18B20.
  if (
    tempC == DEVICE_DISCONNECTED_C ||
    isnan(tempC) ||
    tempC < -55.0 ||
    tempC > 125.0 ||
    fabs(tempC - 85.0) < 0.01
  ) {

    temperatureValid = false;

    Serial.println(
      "Failed to read DS18B20!"
    );

    updateLCD();

    return;
  }

  currentTemperature = tempC;
  temperatureValid = true;

  Serial.print("Temperature: ");
  Serial.print(currentTemperature, 2);
  Serial.println(" C");

  bool abnormal =
    currentTemperature < MIN_TEMP ||
    currentTemperature > MAX_TEMP;

  // Upload early when temperature status changes.
  if (abnormal != wasAbnormal) {

    urgentUpload = true;
  }

  wasAbnormal = abnormal;

  if (abnormal) {

    Serial.println(
      "WARNING: Temperature outside limits!"
    );
  }

  updateLCD();
}

// ===================================
// GENERATE UNIQUE READING KEY
// ===================================

String generateReadingKey() {

  uint32_t deviceId =
    (uint32_t)(ESP.getEfuseMac() & 0xFFFFFFFFULL);

  return String("csiss_") +
         String(deviceId, HEX) +
         "_" +
         String(esp_random(), HEX);
}

// ===================================
// UPLOAD TEMPERATURE TO SUPABASE
// ===================================

bool uploadTemperature(float temperature) {

  if (WiFi.status() != WL_CONNECTED) {

    Serial.println(
      "Upload skipped: WiFi disconnected"
    );

    return false;
  }

  WiFiClientSecure secureClient;

  // DEVELOPMENT TEST ONLY.
  // Replace with certificate verification
  // before real supermarket deployment.
  secureClient.setInsecure();

  HTTPClient http;

  http.setTimeout(12000);

  if (!http.begin(
        secureClient,
        SUPABASE_ENDPOINT
      )) {

    Serial.println(
      "Failed to initialize HTTPS request"
    );

    return false;
  }

  http.addHeader(
    "Content-Type",
    "application/json"
  );

  http.addHeader(
    "X-Device-Key",
    IOT_DEVICE_KEY
  );

  String readingKey = generateReadingKey();

  String payload =
    "{\"reading_key\":\"" +
    readingKey +
    "\",\"temperature\":" +
    String(temperature, 2) +
    "}";

  Serial.println();
  Serial.println("Uploading to Supabase...");

  int httpCode = http.POST(payload);

  String response = http.getString();

  Serial.print("HTTP Status: ");
  Serial.println(httpCode);

  Serial.print("Response: ");
  Serial.println(response);

  http.end();

  if (httpCode == 200 || httpCode == 201) {

    Serial.println("Upload successful!");

    return true;
  }

  Serial.println("Upload failed!");

  return false;
}

// ===================================
// SETUP
// ===================================

void setup() {

  Serial.begin(115200);

  delay(1000);

  Serial.println();
  Serial.println("====================");
  Serial.println("CSISS IoT Monitoring");
  Serial.println("====================");

  // Start LCD
  Wire.begin(LCD_SDA, LCD_SCL);

  lcd.init();
  lcd.backlight();

  lcd.setCursor(0, 0);
  lcd.print("CSISS Monitoring");

  lcd.setCursor(0, 1);
  lcd.print("Starting...");

  // Start DS18B20
  sensors.begin();

  Serial.print("Sensors found: ");
  Serial.println(sensors.getDeviceCount());

  // Start WiFi
  connectWiFi();

  delay(1000);

  lcd.clear();
  updateLCD();
}

// ===================================
// MAIN LOOP
// ===================================

void loop() {

  unsigned long now = millis();

  // ---------------------------------
  // 1. Read sensor every 2 seconds
  // ---------------------------------

  if (
    now - lastSensorRead >= SENSOR_INTERVAL
  ) {

    lastSensorRead = now;

    readTemperature();
  }

  // ---------------------------------
  // 2. Reconnect WiFi when necessary
  // ---------------------------------

  if (
    WiFi.status() != WL_CONNECTED &&
    now - lastWiFiRetry >= WIFI_RETRY_INTERVAL
  ) {

    lastWiFiRetry = now;

    Serial.println(
      "Attempting WiFi reconnection..."
    );

    WiFi.reconnect();

    updateLCD();
  }

  // ---------------------------------
  // 3. Determine when to upload
  // ---------------------------------

  bool scheduledUpload =
    !hasUploaded ||
    (now - lastSuccessfulUpload >= UPLOAD_INTERVAL);

  bool shouldUpload =
    scheduledUpload || urgentUpload;

  bool retryAllowed =
    lastUploadAttempt == 0 ||
    (now - lastUploadAttempt >= RETRY_INTERVAL);

  // ---------------------------------
  // 4. Send valid readings to Supabase
  // ---------------------------------

  if (
    temperatureValid &&
    WiFi.status() == WL_CONNECTED &&
    shouldUpload &&
    retryAllowed
  ) {

    lastUploadAttempt = millis();

    bool success =
      uploadTemperature(currentTemperature);

    if (success) {

      lastSuccessfulUpload = millis();

      hasUploaded = true;
      urgentUpload = false;
    }
  }

  delay(10);
}