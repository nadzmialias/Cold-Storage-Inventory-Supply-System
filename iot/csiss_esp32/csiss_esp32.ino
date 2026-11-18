#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>

#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <math.h>
#include <WebSocketsClient.h>
#include "secrets.h"


const char* SUPABASE_ENDPOINT =
  "https://zujynwdptdmnopfhbuxn.supabase.co"
  "/functions/v1/ingest-temperature";

const char* LIMITS_ENDPOINT =
  "https://zujynwdptdmnopfhbuxn.supabase.co"
  "/functions/v1/get-temperature-limits";

const char* REALTIME_HOST =
  "zujynwdptdmnopfhbuxn.supabase.co";

WebSocketsClient realtimeSocket;
bool realtimeConnected = false;
bool limitsSyncRequested = true;
unsigned long lastRealtimeHeartbeat = 0;
unsigned long realtimeHeartbeatRef = 1;

#define ONE_WIRE_BUS 4 //temperature sensor
#define LCD_SDA 21
#define LCD_SCL 22

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
LiquidCrystal_I2C lcd(0x27, 16, 2);

float minTemp = NAN;
float maxTemp = NAN;
bool limitsLoaded = false;

const unsigned long SENSOR_INTERVAL = 2000; //display temp at lcd hardware every 2 second
const unsigned long UPLOAD_INTERVAL = 60000; //upload to database every 60 second
const unsigned long RETRY_INTERVAL = 10000; //retry every 10 second if error during temp upload to db
const unsigned long WIFI_RETRY_INTERVAL = 15000; //retry every 15 second if error connecting wifi
const unsigned long LIMITS_REFRESH_INTERVAL = 60000; //retrieve temp every 60 second
const unsigned long LIMITS_FALLBACK_INTERVAL = //check latest limit(max/min) every 15 min from db if realtime update is missed
  15UL * 60UL * 1000UL;
const unsigned long LIMITS_RETRY_INTERVAL = 10000; //
const unsigned long REALTIME_HEARTBEAT_INTERVAL = 20000; //send message to database to keep WebSocket connection active every 20 second

float currentTemperature = NAN;

bool temperatureValid = false;
bool wasAbnormal = false;
bool urgentUpload = false;
bool hasUploaded = false;

unsigned long lastSensorRead = 0;
unsigned long lastSuccessfulUpload = 0;
unsigned long lastUploadAttempt = 0;
unsigned long lastWiFiRetry = 0;
unsigned long lastLimitsFetchAttempt = 0;
unsigned long lastLimitsFetchSuccess = 0;

void connectWiFi() {

  WiFi.mode(WIFI_STA);

  WiFi.begin(
    WIFI_SSID,
    WIFI_PASSWORD
  );

  Serial.println("Connecting to WiFi...");
}

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


bool fetchTemperatureLimits() {
  if (WiFi.status() != WL_CONNECTED) {
    return false;
  }

  WiFiClientSecure secureClient;

  // Development configuration, matching the existing upload code.
  secureClient.setInsecure();

  HTTPClient http;
  http.setTimeout(12000);

  if (!http.begin(secureClient, LIMITS_ENDPOINT)) {
    Serial.println("Failed to connect to limits endpoint");
    return false;
  }

  http.addHeader("X-Device-Key", IOT_DEVICE_KEY);

  int httpCode = http.GET();

  if (httpCode != HTTP_CODE_OK) {
    Serial.printf(
  "[Limits] GET failed: %d (%s)\n",
  httpCode,
  http.errorToString(httpCode).c_str()
);

Serial.printf(
  "[Debug] WiFi RSSI: %d dBm | Free heap: %u bytes\n",
  WiFi.RSSI(),
  ESP.getFreeHeap()
);
    http.end();
    return false;
  }

  String response = http.getString();
  http.end();

  float newMin = NAN;
  float newMax = NAN;

  if (
    sscanf(response.c_str(), "%f,%f", &newMin, &newMax) != 2 ||
    !isfinite(newMin) ||
    !isfinite(newMax) ||
    newMin >= newMax
  ) {
    Serial.println("Invalid limits response");
    return false;
  }

  

bool limitsChanged =
  !limitsLoaded ||
  fabs(newMin - minTemp) > 0.001f ||
  fabs(newMax - maxTemp) > 0.001f;

if (limitsChanged) {
  minTemp = newMin;
  maxTemp = newMax;
  limitsLoaded = true;

  Serial.printf(
    "[Limits] Updated: %.2f C to %.2f C\n",
    minTemp,
    maxTemp
  );

  if (temperatureValid) {
    bool abnormal =
      currentTemperature < minTemp ||
      currentTemperature > maxTemp;

    if (abnormal != wasAbnormal) {
      urgentUpload = true;
    }

    wasAbnormal = abnormal;
  }
} else {
  Serial.println("[Limits] Temperature limits unchanged.");
}

return true;

}


void readTemperature() {

  sensors.requestTemperatures();
  float tempC = sensors.getTempCByIndex(0);

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


if (limitsLoaded) {
  bool abnormal =
    currentTemperature < minTemp ||
    currentTemperature > maxTemp;

  // Upload immediately if temperature status changes.
  if (abnormal != wasAbnormal) {
    urgentUpload = true;
  }

  wasAbnormal = abnormal;

  if (abnormal) {
    Serial.println(
      "WARNING: Temperature outside limits!"
    );
  }
}

updateLCD();

}

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



void onRealtimeEvent(
  WStype_t type,
  uint8_t* payload,
  size_t length
) {
  switch (type) {

    case WStype_CONNECTED:
      limitsSyncRequested = true;
      realtimeConnected = true;
      lastRealtimeHeartbeat = millis();
      realtimeHeartbeatRef = 1;

      Serial.println("[Realtime] WebSocket connected!");

      // Join the public cold-storage:1 channel.
      realtimeSocket.sendTXT(
        R"json({
          "topic":"realtime:cold-storage:1",
          "event":"phx_join",
          "payload":{
            "config":{
              "broadcast":{"ack":false,"self":false},
              "presence":{"enabled":false},
              "private":false
            }
          },
          "ref":"1",
          "join_ref":"1"
        })json"
      );

      Serial.println("[Realtime] Join request sent.");
      break;

    
  case WStype_TEXT: {
    String message;
    message.reserve(length + 1);

    for (size_t i = 0; i < length; i++) {
      message += (char)payload[i];
    }

    Serial.print("[Realtime] Received: ");
    Serial.println(message);

    // Detect the temperature limit change notification.
    if (
      message.indexOf("\"topic\":\"realtime:cold-storage:1\"") >= 0 &&
      message.indexOf("\"event\":\"broadcast\"") >= 0 &&
      message.indexOf("\"event\":\"limits_changed\"") >= 0
    ) {
      Serial.println(
        "[Realtime] Temperature limits change detected!"
      );

      limitsSyncRequested = true;
    }

    break;
  }


    case WStype_DISCONNECTED:
      realtimeConnected = false;
      Serial.println("[Realtime] WebSocket disconnected.");
      break;

    case WStype_ERROR:
      Serial.println("[Realtime] WebSocket error.");
      break;

    default:
      break;
  }
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

  String realtimePath =
    "/realtime/v1/websocket?apikey=" +
    String(SUPABASE_PUBLISHABLE_KEY) +
    "&vsn=1.0.0";

  realtimeSocket.beginSSL(
    REALTIME_HOST,
    443,
    realtimePath.c_str(),
    nullptr,
    ""
  );

  realtimeSocket.onEvent(onRealtimeEvent);

  realtimeSocket.setReconnectInterval(5000);

  delay(1000);

  lcd.clear();
  updateLCD();
}

// ===================================
// MAIN LOOP
// ===================================

void loop() {

  unsigned long now = millis();
  realtimeSocket.loop();

  if (
    realtimeConnected &&
    millis() - lastRealtimeHeartbeat >=
      REALTIME_HEARTBEAT_INTERVAL
  ) {
    lastRealtimeHeartbeat = millis();

    String heartbeat =
      "{\"topic\":\"phoenix\","
      "\"event\":\"heartbeat\","
      "\"payload\":{},"
      "\"ref\":\"" +
      String(++realtimeHeartbeatRef) +
      "\"}";

    realtimeSocket.sendTXT(heartbeat);
  }


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


if (
  WiFi.status() == WL_CONNECTED &&
  limitsSyncRequested &&
  (
    lastLimitsFetchAttempt == 0 ||
    millis() - lastLimitsFetchAttempt >= LIMITS_RETRY_INTERVAL
  )
) {
  lastLimitsFetchAttempt = millis();

  Serial.println("[Limits] Synchronizing with Supabase...");

  if (fetchTemperatureLimits()) {
    lastLimitsFetchSuccess = millis();
    limitsSyncRequested = false;

    Serial.println("[Limits] Synchronization successful!");
  } else {
    Serial.println("[Limits] Sync failed. Will retry.");
  }
}


if (
  limitsLoaded &&
  millis() - lastLimitsFetchSuccess >=
    LIMITS_FALLBACK_INTERVAL
) {
  limitsSyncRequested = true;
}


// Check for updated temperature limits.
bool limitsDue =
  !limitsLoaded ||
  (now - lastLimitsFetchSuccess >= LIMITS_REFRESH_INTERVAL);

bool limitsRetryAllowed =
  lastLimitsFetchAttempt == 0 ||
  (now - lastLimitsFetchAttempt >= LIMITS_RETRY_INTERVAL);

if (
  WiFi.status() == WL_CONNECTED &&
  limitsDue &&
  limitsRetryAllowed
) {
  lastLimitsFetchAttempt = millis();

  if (fetchTemperatureLimits()) {
    lastLimitsFetchSuccess = millis();
  }
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