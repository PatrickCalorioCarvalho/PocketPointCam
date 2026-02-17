package com.example.pocketpointcam_app

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnConnect: Button
    private lateinit var btnPhoto: Button
    private lateinit var btnFlash: Button
    private lateinit var txtStatus: TextView
    private lateinit var imageView: ImageView
    private lateinit var txtProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingSpinner: ProgressBar

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var gatt: BluetoothGatt? = null
    private var flashOn = false
    private val photoBuffer = ByteArrayOutputStream()

    // UUIDs iguais aos do ESP32
    private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val PHOTO_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ac")
    private val FLASH_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ad")
    private val ACK_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ae")
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    private val requestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConnect = findViewById(R.id.btnConnect)
        btnPhoto = findViewById(R.id.btnPhoto)
        btnFlash = findViewById(R.id.btnFlash)
        txtStatus = findViewById(R.id.txtStatus)
        imageView = findViewById(R.id.imageView)
        txtProgress = findViewById(R.id.txtProgress)
        progressBar = findViewById(R.id.progressBar)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        enableCameraButtons(false)
        progressBar.visibility = View.GONE
        loadingSpinner.visibility = View.GONE

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        btnConnect.setOnClickListener {
            txtStatus.text = "🔄 Verificando permissões..."
            checkPermissions()
        }

        btnFlash.setOnClickListener {
            flashOn = !flashOn
            toggleFlash(flashOn)
            btnFlash.text = if (flashOn) "💡 Flash OFF" else "💡 Flash ON"
        }

        btnPhoto.setOnClickListener {
            capturePhoto()
        }
    }

    private fun checkPermissions() {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), requestCode)
        } else {
            startBleScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBleScan()
            } else {
                txtStatus.text = "❌ Permissões necessárias não concedidas"
            }
        }
    }

    private fun startBleScan() {
        txtStatus.text = "🔍 Procurando câmera BLE..."
        loadingSpinner.visibility = View.VISIBLE
        txtProgress.text = "Buscando dispositivo..."
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
                val scanner = bluetoothAdapter.bluetoothLeScanner
                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val device = result.device
                        if (device.name == "PocketPointCam_BLE") {
                            txtStatus.text = "✅ Câmera encontrada, conectando..."
                            scanner.stopScan(this)
                            connectToDevice(device)
                        }
                    }
                }
                scanner.startScan(scanCallback)
            } else {
                txtStatus.text = "❌ Permissão BLUETOOTH_SCAN não concedida"
            }
        } catch (e: SecurityException) {
            txtStatus.text = "⚠️ Erro de permissão: ${e.message}"
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
                gatt = device.connectGatt(this, false, gattCallback)
            } else {
                txtStatus.text = "❌ Permissão BLUETOOTH_CONNECT não concedida"
            }
        } catch (e: SecurityException) {
            txtStatus.text = "⚠️ Erro de permissão: ${e.message}"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                    txtStatus.text = "✅ Conectado à câmera BLE"
                    txtProgress.text = ""
                    enableCameraButtons(true)

                }
                try {
                    if (ContextCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.requestMtu(510)
                        gatt.discoverServices()
                    } else {
                        txtStatus.text = "❌ Permissão BLUETOOTH_CONNECT não concedida"
                    }
                } catch (e: SecurityException) {
                    txtStatus.text = "⚠️ Erro de permissão: ${e.message}"
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    txtStatus.text = "❌ Desconectado"
                    txtProgress.text = ""
                    enableCameraButtons(false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    if (ContextCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                        val service = gatt.getService(SERVICE_UUID)
                        val photoChar = service?.getCharacteristic(PHOTO_UUID)

                        if (photoChar != null) {
                            gatt.setCharacteristicNotification(photoChar, true)

                            val descriptor = photoChar.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            )
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)

                            Log.i("PocketPointCam", "Notificações habilitadas para PHOTO_UUID")
                        }
                    } else {
                        txtStatus.text = "❌ Permissão BLUETOOTH_CONNECT não concedida"
                    }
                } catch (e: SecurityException) {
                    txtStatus.text = "⚠️ Erro de permissão: ${e.message}"
                }
            }
        }
        private val photoBuffer = ByteArrayOutputStream()
        private var expectedSize = -1
        private var finishedFlag = false

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == PHOTO_UUID) {
                val data = characteristic.value
                val str = String(data)

                if (expectedSize == -1 && str.toIntOrNull() != null) {
                    expectedSize = str.toInt()
                    photoBuffer.reset()
                    finishedFlag = false
                    runOnUiThread {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = 0
                        txtProgress.text = "Recebendo imagem..."
                    }
                    Log.i("PocketPointCam", "Esperando $expectedSize bytes")
                    sendAck(gatt, "NEXT") // pede primeiro chunk
                    return
                }

                if (str == "END") {
                    finishedFlag = true
                    Log.i("PocketPointCam", "Flag END recebido")
                    tryDecode()
                    return
                }

                synchronized(photoBuffer) {
                    photoBuffer.write(data)
                }

                val percent = (photoBuffer.size() * 100) / expectedSize
                runOnUiThread {
                    progressBar.progress = percent
                    txtProgress.text = "Recebendo imagem... $percent%"
                }
                Log.d("PocketPointCam", "Buffer atual: ${photoBuffer.size()} / $expectedSize")

                // pede próximo chunk
                sendAck(gatt, "NEXT")
                tryDecode()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        private fun sendAck(gatt: BluetoothGatt, msg: String) {
            val ackChar = gatt.getService(SERVICE_UUID).getCharacteristic(ACK_UUID)
            ackChar.value = msg.toByteArray()
            gatt.writeCharacteristic(ackChar)
        }

        private fun tryDecode() {
            if (finishedFlag && expectedSize > 0 && photoBuffer.size() == expectedSize) {
                val bytes = photoBuffer.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    loadingSpinner.visibility = View.GONE
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        txtStatus.text = "📸 Foto recebida (${bytes.size} bytes)"
                        txtProgress.text = ""
                    } else {
                        txtStatus.text = "❌ Falha ao decodificar imagem"
                    }
                }
                expectedSize = -1
                finishedFlag = false
                photoBuffer.reset()
            }
        }
    }

    private fun toggleFlash(on: Boolean) {
        try {
            val service = gatt?.getService(SERVICE_UUID)
            val flashChar = service?.getCharacteristic(FLASH_UUID)
            flashChar?.value = if (on) "ON".toByteArray() else "OFF".toByteArray()
            gatt?.writeCharacteristic(flashChar)
        } catch (e: SecurityException) {
            txtStatus.text = "⚠️ Erro de permissão: ${e.message}"
        }
    }

    private fun capturePhoto() {
        try {
            val service = gatt?.getService(SERVICE_UUID)
            val photoChar = service?.getCharacteristic(PHOTO_UUID)
            photoBuffer.reset()
            photoChar?.value = "CAPTURE".toByteArray()
            gatt?.writeCharacteristic(photoChar)

            Thread {
                Thread.sleep(2000) // aguarda chunks
                val bytes = photoBuffer.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            }.start()
        } catch (e: SecurityException) {
            txtStatus.text = "⚠️ Erro de permissão: ${e.message}"
        }
    }

    private fun enableCameraButtons(enable: Boolean) {
        btnConnect.isEnabled = !enable
        btnPhoto.isEnabled = enable
        btnFlash.isEnabled = enable
    }
}
