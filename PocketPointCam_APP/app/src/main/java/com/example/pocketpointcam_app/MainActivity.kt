package com.example.pocketpointcam_app

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var btnConnect: Button
    private lateinit var btnPhoto: Button
    private lateinit var btnFlash: Button
    private lateinit var txtStatus: TextView
    private lateinit var imageView : ImageView
    private val CAMERA_IP = "http://192.168.4.1"
    private val STATUS_URL = "$CAMERA_IP/status"
    private var flashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConnect = findViewById(R.id.btnConnect)
        btnPhoto = findViewById(R.id.btnPhoto)
        btnFlash = findViewById(R.id.btnFlash)
        txtStatus = findViewById(R.id.txtStatus)
        imageView = findViewById<ImageView>(R.id.imageView)

        enableCameraButtons(false)

        btnConnect.setOnClickListener {
            checkCameraConnection()
        }

        btnPhoto.setOnClickListener {
            thread {
                try {
                    val url = URL("$CAMERA_IP/photo")
                    val stream = url.openStream()
                    val bitmap = BitmapFactory.decodeStream(stream)

                    runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        btnFlash.setOnClickListener {
            flashOn = !flashOn
            val endpoint = if (flashOn) "/flash/on" else "/flash/off"
            val buttonText = if (flashOn) "💡 Flash OFF" else "💡 Flash ON"

            thread {
                try {
                    URL("$CAMERA_IP$endpoint").openStream().close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            btnFlash.text = buttonText
        }
    }

    private fun checkCameraConnection() {

        txtStatus.text = "🔄 Conectando à câmera..."

        Thread {
            try {
                val url = URL(STATUS_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2000
                conn.readTimeout = 2000

                val responseCode = conn.responseCode

                runOnUiThread {
                    if (responseCode == 200) {
                        txtStatus.text = "✅ Câmera conectada"
                        enableCameraButtons(true)
                    } else {
                        onCameraNotConnected()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    onCameraNotConnected()
                }
            }
        }.start()
    }

    private fun onCameraNotConnected() {
        txtStatus.text = "❌ Câmera não conectada"
        enableCameraButtons(false)
        showConnectionHelpDialog()
    }

    private fun enableCameraButtons(enable: Boolean) {
        btnPhoto.isEnabled = enable
        btnFlash.isEnabled = enable
    }

    private fun showConnectionHelpDialog() {

        val message = """
            Para conectar à câmera:

            1️⃣ Abra o Wi-Fi do celular
            2️⃣ Conecte na rede:
            
            SSID:
            PocketPointCam_AP

            Senha:
            *PocketPointCam@2026

            3️⃣ Aguarde conectar
            4️⃣ Volte ao app
            5️⃣ Toque em "Conectar câmera"

            IP da câmera:
            192.168.4.1
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Câmera não encontrada")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
