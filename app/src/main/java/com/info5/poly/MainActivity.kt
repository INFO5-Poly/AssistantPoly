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
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.info5.poly.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val JS_OBJ_NAME = "AndroidAPI"
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var textToSpeechIsInitialized = false
    private lateinit var speechRecognizerIntent: Intent
    private lateinit var api: WebAPI;
    private val permissionsToAcquire: MutableList<String> = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it)
            permissionsToAcquire.removeAt(0)

        requestPermissions()
    }

    private inner class WebAndroidAPI {
        private var isListening: Boolean = false

        @JavascriptInterface
        fun setListening(listening: Boolean){
            runOnUiThread {
                if(this.isListening && !listening){
                    stop_listening()
                }
                else if(!this.isListening && listening){
                    listen_voice()
                }

                this.isListening = listening
            }
        }
    }

    private inner class WebAPI(private val webView: WebView) {
        private fun escapeJS(str: String): String{
            return str
                .replace("'", "\\'")
                .replace("\n", "\\n")
        }

        fun addMessage(user: Boolean){
            webView.evaluateJavascript("addMessage(${user});") {}
        }

        fun editMessage(msg: String){
            webView.evaluateJavascript("editMessage('${escapeJS(msg)}');") {}
        }

        fun deleteMessage(){
            webView.evaluateJavascript("deleteMessage();") {}
        }

        fun setListening(listening: Boolean){
            webView.evaluateJavascript("setListening(${listening});") {}
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAndroidAPI(), JS_OBJ_NAME)
        webView.loadUrl("file:///android_asset/web/poly.html")

        webView.webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                Log.d("WebViewConsole", message!!)
            }
        }
        initSpeechRecognition()
        initSpeechSynthesis()
        api = WebAPI(webView)
    }

    fun initSpeechRecognition(){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something")

        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle) {
                // Called when the recognizer is ready for the user to start speaking
            }

            override fun onBeginningOfSpeech() {
                // Called when the user starts speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Called when the volume of the user's speech changes
            }

            override fun onBufferReceived(buffer: ByteArray) {
                // Called when the recognizer has received audio input
            }

            override fun onEndOfSpeech() {
                api.setListening(false)
            }

            override fun onError(error: Int) {
                // Called when an error occurs
            }

            override fun onResults(results: Bundle) {
                // Called when the recognizer has successfully recognized the user's speech
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.size > 0) {
                    val spokenText = matches[0]
                    // Do something with the recognized text
                    api.editMessage(spokenText)

                    //The response of Poly
                    api.addMessage(false)
                    val polyResponse = "Réponse de Poly"
                    api.editMessage(polyResponse)
                    // Use the TextToSpeech object to speak the text
                    textToSpeech?.speak(polyResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.size > 0) {
                    val spokenText = matches[0]
                    api.editMessage(spokenText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle) {
                // Called when a recognition event occurs
            }
        })
    }


    fun initSpeechSynthesis(){
        // Initialize the TextToSpeech object
        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                }
                else {
                    Toast.makeText(this, "Text to Speech is initialized", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        })
    }

    fun phoneCall(phoneNumber: String) {
        val callIntent: Intent = Uri.parse(phoneNumber).let { number ->
            Intent(Intent.ACTION_CALL, number)
        }
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            val requestCode = 1
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.CALL_PHONE),
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

    @SuppressLint("QueryPermissionsNeeded")
    fun listen_voice() {
        api.addMessage(true)
        speechRecognizer?.startListening(speechRecognizerIntent)

    }

    fun stop_listening() {
        api.deleteMessage()
        speechRecognizer?.cancel()

    }

    private fun requestPermissions(){
        if(permissionsToAcquire.isEmpty())
            return
        var permission = permissionsToAcquire[0]

        while(ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED){
            permissionsToAcquire.removeAt(0)
            if(permissionsToAcquire.isEmpty())
                return
            permission = permissionsToAcquire[0]
        }
        permissionLauncher.launch(permission)

    }

    /*override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for the TextToSpeech object
            val result = textToSpeech?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // The language is not supported
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
            }
            else {
                // Use the TextToSpeech object to speak the text
                // textToSpeech?.speak("enter the text", TextToSpeech.QUEUE_FLUSH, null, null)
                Toast.makeText(this, "Text to speech is initialized", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Initialization failed
            Toast.makeText(this, "Text-to-speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }*/

    override fun onDestroy() {
        // Shutdown the TextToSpeech object when the activity is destroyed
        super.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }


}