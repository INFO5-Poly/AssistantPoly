package com.info5.poly

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.internal.ContextUtils.getActivity
import com.info5.poly.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val JS_OBJ_NAME = "AndroidAPI"
    private lateinit var python: PythonExecutor

    private inner class WebAndroidAPI {
        private var isRecording: Boolean = false
        private var speechRecognizer : SpeechRecognizer? = null //Speech recognizer

        @JavascriptInterface
        fun setRecording(recording: Boolean){
            this.isRecording = recording
            Log.d("Poly Debug", "recording: " + this.isRecording)


            // TEST =========
            var code = "def fibonacci_of(n):\n"
            code += "   if n in {0, 1}:  # Base case\n"
            code += "       return n\n"
            code += "   return n\n"
            code += "\n"
            code += "print([fibonacci_of(n) for n in range(10)])\n"

            python.fromString(code){
                Log.d("PythonOutput", it)
            }
            //================
        }

        /*@JavascriptInterface
        fun handlePermissions () {
            // Phone call
            if (ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                val requestCode = 1
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(android.Manifest.permission.CALL_PHONE),
                    requestCode
                )
            }

            // Audio recording
            if (ContextCompat.checkSelfPermission
                    (applicationContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    val recordAudioRequestCode = 1
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO),
                        recordAudioRequestCode
                    )
                }
            }
        }*/

        @JavascriptInterface
        fun phoneCall(phoneNumber: String) {
            val callIntent: Intent = Uri.parse(phoneNumber).let { number ->
                Intent(Intent.ACTION_CALL, number)
            }
            if (ContextCompat.checkSelfPermission(applicationContext,android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                val requestCode = 1
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(android.Manifest.permission.CALL_PHONE),
                    requestCode
                )
            } else {
                try {
                    startActivity(callIntent)
                } catch (e: ActivityNotFoundException) {
                    // Define what your app should do if no activity can handle the intent.
                }
            }
        }

        @JavascriptInterface
        fun speechToText() : String {
            // Permissions for audio recording
            if (ContextCompat.checkSelfPermission
                    (applicationContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    val recordAudioRequestCode = 1
                    ActivityCompat.requestPermissions(
                        this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO),
                        recordAudioRequestCode
                    )
                }
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            var recognizedText : String = "Texte par défaut..."
            val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            // Recognition listener
            speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(p0: Float) {}

                override fun onBufferReceived(p0: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(p0: Int) {}

                override fun onResults(bundle: Bundle?) {
                    val data = bundle!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    recognizedText = data!![0]
                }

                override fun onPartialResults(p0: Bundle?) {}

                override fun onEvent(p0: Int, p1: Bundle?) {}
            })

            speechRecognizer!!.startListening(speechRecognizerIntent)

            return recognizedText
        }
    }

    private inner class WebAPI(private val webView: WebView) {
        private fun escapeJS(str: String): String{
            return str
                .replace("'", "\'")
                .replace("\n", "\\n")
        }
        fun addMessage(user: Boolean){
            webView.evaluateJavascript("addMessage(${user};") { messageID ->

            }
        }
        fun editMessage(user: Boolean, id: Int, msg: String){
            webView.evaluateJavascript("editMessage(${user}, ${id}, '${escapeJS(msg)}';") {}
        }

        fun setLoading(loading: Boolean){
            webView.evaluateJavascript("setLoading(${loading};") {}
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        python = PythonExecutor(applicationContext)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAndroidAPI(), JS_OBJ_NAME)
        webView.loadUrl("file:///android_asset/web/poly.html")

        webView.webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                Log.d("WebViewConsole", message!!)
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val recordAudioRequestCode = 1
        if (requestCode == recordAudioRequestCode
            && grantResults.isNotEmpty())
        {
            Toast.makeText(this, "Permission Granted...", Toast.LENGTH_SHORT).show()
        }
    }


}