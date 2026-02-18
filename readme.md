# 📷 PocketPointCam (BLE Version)

PocketPointCam é um projeto **IoT + Mobile** que transforma um
**ESP32-CAM** em uma câmera portátil controlada por um aplicativo
**Android** utilizando **Bluetooth Low Energy (BLE)**.

A comunicação é feita via **GATT**, permitindo:

-   📡 Conexão direta via BLE (sem Wi-Fi)
-   📸 Envio de imagens em **pacotes fragmentados**
-   ✅ Controle de **ACK (confirmação de recebimento)**
-   📦 Ajuste de **MTU** para otimizar throughput
-   💡 Controle de flash via característica BLE
-   🔒 100% offline, sem internet

------------------------------------------------------------------------

## 🧩 Arquitetura

    [ Android App ]
            |
            |   BLE (GATT)
            v
    [ ESP32-CAM ]
       |- Service: CameraService
           |- Characteristic: Status
           |- Characteristic: PhotoChunk
           |- Characteristic: FlashControl
           |- Characteristic: Ack

------------------------------------------------------------------------

## 🚀 Visão Geral

-   📶 ESP32-CAM atua como **BLE Peripheral**
-   📱 Android atua como **BLE Central**
-   🔍 App descobre dispositivo `PocketPointCam`
-   🔗 Conexão GATT estabelecida
-   📏 Negociação de MTU (ex: 247 bytes)
-   📸 Foto capturada sob comando
-   📦 Imagem dividida em múltiplos pacotes
-   ✅ Cada pacote exige confirmação (ACK)
-   🔄 Reenvio automático em caso de falha

------------------------------------------------------------------------

## 📦 Tecnologias Utilizadas

### ESP32-CAM

-   Arduino Framework
-   ESP32 BLE Library
-   esp_camera
-   Fragmentação manual de buffer JPEG

### Android

-   Kotlin
-   Android Studio
-   BluetoothLeScanner
-   BluetoothGatt
-   Negotiated MTU
-   Controle manual de ACK

------------------------------------------------------------------------

## 📡 Estrutura BLE (GATT)

### 🔹 Service UUID

    Camera Service UUID

### 🔹 Characteristics

#### ✅ Status

-   Tipo: Read / Notify
-   Retorno:

``` json
{"status":"ok"}
```

------------------------------------------------------------------------

#### 📸 PhotoChunk

-   Tipo: Notify
-   Envia fragmentos da imagem JPEG
-   Tamanho do pacote baseado no MTU negociado

------------------------------------------------------------------------

#### 💡 FlashControl

-   Tipo: Write
-   Valores:
    -   `0x01` → Flash ON
    -   `0x00` → Flash OFF

------------------------------------------------------------------------

#### 🔁 Ack

-   Tipo: Write
-   Usado pelo app para confirmar recebimento de cada pacote
-   Caso o ACK não seja recebido, o pacote é reenviado

------------------------------------------------------------------------

## 📱 Funcionalidades do App Android

-   🔍 Scan BLE para encontrar `PocketPointCam`
-   🔗 Conexão GATT
-   📏 Solicitação de MTU máximo suportado
-   📸 Botão **Tirar Foto**
-   💡 Botão **Flash ON/OFF**
-   📦 Reconstrução da imagem a partir dos chunks recebidos
-   💾 (Planejado) Salvar imagem no armazenamento local

------------------------------------------------------------------------

## 🔄 Fluxo de Envio da Imagem

1.  App envia comando "Capturar"
2.  ESP32-CAM captura imagem JPEG
3.  Buffer é dividido em pacotes menores (MTU - overhead)
4.  ESP envia pacote via Notify
5.  App envia ACK
6.  Próximo pacote é enviado
7.  Processo repete até finalizar imagem

------------------------------------------------------------------------

## 🛠️ Como Rodar o Projeto

### ESP32-CAM

1.  Abra o projeto no Arduino IDE
2.  Selecione a placa **AI Thinker ESP32-CAM**
3.  Configure a porta correta
4.  Faça upload do firmware

------------------------------------------------------------------------

### Android App

1.  Abra o projeto no Android Studio
2.  Verifique permissões BLE no `AndroidManifest.xml`
3.  Ative Bluetooth no celular
4.  Abra o app e conecte ao dispositivo `PocketPointCam`

------------------------------------------------------------------------

## 🔐 Permissões Android

O aplicativo utiliza: - Bluetooth - Bluetooth Admin - Bluetooth Scan /
Connect (Android 12+)

❌ Não utiliza: - Internet - Localização (exceto se exigido pela versão
do Android para scan BLE)

------------------------------------------------------------------------

## 🧪 Status do Projeto

-   ✅ Conexão BLE
-   ✅ Negociação de MTU
-   ✅ Envio fragmentado com ACK
-   ⏳ Compressão adaptativa baseada no MTU
-   ⏳ Preview ao vivo via stream BLE (experimental)

------------------------------------------------------------------------

## 📌 Próximas Ideias

-   Ajuste dinâmico de qualidade JPEG
-   Controle de taxa de envio (throttling)
-   Modo burst
-   App multiplataforma (Flutter)

------------------------------------------------------------------------

## 🤝 Contribuição

Sinta-se à vontade para abrir **issues**, **pull requests** ou sugerir
melhorias.

------------------------------------------------------------------------

## 📄 Licença

Projeto open-source para fins educacionais e experimentais.

------------------------------------------------------------------------

👤 Autor: Patrick Calorio\
Projeto criado para estudos em **IoT, BLE, sistemas embarcados e
mobile**.
