#include "esp_camera.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#define CAMERA_MODEL_AI_THINKER 
#include "camera_pins.h"

#define FLASH_PIN 4

#define SERVICE_UUID        "12345678-1234-1234-1234-1234567890ab"
#define PHOTO_UUID          "12345678-1234-1234-1234-1234567890ac"
#define FLASH_UUID          "12345678-1234-1234-1234-1234567890ad"
#define ACK_UUID            "12345678-1234-1234-1234-1234567890ae"

BLEServer *pServer;
BLECharacteristic *photoChar;
BLECharacteristic *flashChar;
BLECharacteristic *ackChar;
bool deviceConnected = false;

class FlashCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String value = pCharacteristic->getValue();  // usar String do Arduino
    if (value == "ON") {
      digitalWrite(FLASH_PIN, HIGH);
      Serial.println("Flash ON");
    } else {
      digitalWrite(FLASH_PIN, LOW);
      Serial.println("Flash OFF");
    }
  }
};


void startCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size = FRAMESIZE_VGA;
  config.jpeg_quality = 10;
  config.fb_count = 1;

  esp_camera_init(&config);
}

int currentIndex = 0;
int chunkSize = 500;
camera_fb_t * fb;

class AckCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    String value = pCharacteristic->getValue();
    if (value == "NEXT") {
      sendNextChunk();
    }
  }
};

void sendNextChunk() {
  if (!fb) return;

  if (currentIndex < fb->len) {
    int len = (currentIndex + chunkSize < fb->len) ? chunkSize : (fb->len - currentIndex);
    photoChar->setValue(fb->buf + currentIndex, len);
    photoChar->notify();
    currentIndex += len;
    Serial.printf("Chunk enviado: %d/%d\n", currentIndex, fb->len);
  } else {
    // terminou, envia END
    photoChar->setValue("END");
    photoChar->notify();
    esp_camera_fb_return(fb);
    fb = nullptr;
    Serial.println("Foto enviada com END");
  }
}


void sendPhotoBLE() {
  fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("Erro ao capturar foto");
    return;
  }

  char sizeStr[16];
  sprintf(sizeStr, "%d", fb->len);
  photoChar->setValue(sizeStr);
  photoChar->notify();
  currentIndex = 0;
}





class PhotoCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String value = pCharacteristic->getValue();
    if (value == "CAPTURE") {
      sendPhotoBLE();
    }
  }
};


void setup() {
  Serial.begin(115200);

  pinMode(FLASH_PIN, OUTPUT);
  digitalWrite(FLASH_PIN, LOW);

  startCamera();

  BLEDevice::init("PocketPointCam_BLE");
  pServer = BLEDevice::createServer();

  BLEService *pService = pServer->createService(SERVICE_UUID);

  flashChar = pService->createCharacteristic(
    FLASH_UUID,
    BLECharacteristic::PROPERTY_WRITE
  );
  flashChar->setCallbacks(new FlashCallbacks());
  
  ackChar = pService->createCharacteristic(
      ACK_UUID,
      BLECharacteristic::PROPERTY_WRITE
  );
  ackChar->setCallbacks(new AckCallbacks());



  photoChar = pService->createCharacteristic(
    PHOTO_UUID,
    BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY
  );
  photoChar->addDescriptor(new BLE2902());
  photoChar->setCallbacks(new PhotoCallbacks());

  pService->start();
  pServer->getAdvertising()->start();

  Serial.println("BLE iniciado. Procure por PocketPointCam_BLE");
}

void loop() {

}
