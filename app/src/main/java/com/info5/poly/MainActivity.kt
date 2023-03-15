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
import android.provider.AlarmClock
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.info5.poly.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val JS_OBJ_NAME = "AndroidAPI"
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var speechRecognizerIntent: Intent
    private lateinit var api: WebAPI
    private lateinit var retrofit:Retrofit
    private lateinit var bot: ChatGPTService
    private lateinit var key: String
    private enum class ChatState{
        IDLE,
        LISTENING,
        WAITING,
    }
    private var state: ChatState = ChatState.IDLE
    private val permissionsToAcquire: MutableList<String> = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val apiKeyFilename: String = "openai.key"

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it)
            permissionsToAcquire.removeAt(0)

        requestPermissions()
    }

    private inner class WebAndroidAPI {


        @JavascriptInterface
        fun setListening(listening: Boolean){
            runOnUiThread {
                if(this@MainActivity.state == ChatState.LISTENING && !listening){
                    stop_listening()
                }
                else if(this@MainActivity.state == ChatState.IDLE && listening){
                    listen_voice()
                }

            }
        }

        @JavascriptInterface
        fun apiKeyChanged(key: String){
            val directoryPath = applicationContext.filesDir
            val filePath = directoryPath.toString().plus("/").plus(apiKeyFilename)

            lifecycleScope.launch(Dispatchers.IO) {
                var file = File(filePath)
                file.writeText(key)
                Log.d("API_KEY", key)
                bot.setKey(Key(key))!!.execute()
            }
        }

    }

    fun readApiKeyFile(): String{
        val directoryPath = applicationContext.filesDir
        val filePath = directoryPath.toString().plus("/").plus(apiKeyFilename)
        val file = File(filePath)
        if(file.isFile){
            return file.readText()
        }
        return ""
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

    data class Message(
        val msg: String
    )
    data class Received(
        val msg: String,
        val complete: Boolean
    )
    data class Key(
        val key: String
    )

    interface ChatGPTService {
        @POST("message")
        fun send(@Body message: Message): Call<Void>?

        @POST("key")
        fun setKey(@Body key: Key): Call<Void>?

        @POST("reset")
        fun reset(): Call<Void>?

        @GET("response")
        fun getResponse(): Call<Received>?
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permissions for audio recording
        if (ContextCompat.checkSelfPermission
                (
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val recordAudioRequestCode = 1
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO),
                    recordAudioRequestCode
                )
            }
        }

        this.key = readApiKeyFile()
        Log.d("API_KEY", key)
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAndroidAPI(), JS_OBJ_NAME)
        api = WebAPI(webView)
        webView.loadUrl("file:///android_asset/web/poly.html")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                Log.d("WebViewConsole", message!!)
            }
        }
        initSpeechRecognition()
        api = WebAPI(webView)
        retrofit = Retrofit.Builder()
            .baseUrl("http://polyserver.francecentral.cloudapp.azure.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        bot = retrofit.create(ChatGPTService::class.java)
        if (key != ""){
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val response = bot.setKey(Key(key))!!.execute()
                        if (!response.isSuccessful) {
                            Log.d("KEY-RESPONSE", response.errorBody()!!.string())
                        }
                    } catch (e: Exception) {
                        Log.d("KEY-RESPONSE", e.toString())
                    }
                }
            }
        }
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

                    sendMessage(spokenText)
                    api.editMessage(spokenText)
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
    fun sendMessage(message: String){
        this.state = ChatState.WAITING
        var done = false
        var body: String = ""

        fun getResponse(){
            var received = bot.getResponse()!!.execute().body()!!
            done = received.complete
            body = received.msg
        }
        bot.send(Message(message))
        api.addMessage(false)
        lifecycleScope.launch(Dispatchers.IO) {
            while(!done){
                getResponse()
                api.editMessage(body)
            }
            // update UI with the result using the main thread dispatcher
            withContext(Dispatchers.Main) {
                this@MainActivity.state = ChatState.IDLE
            }
        }
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

    fun setAlarm(hour:Int,minute:Int,message:String){
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour) // Set the hour to 8am
            putExtra(AlarmClock.EXTRA_MINUTES, minute) // Set the minute to 30
            putExtra(AlarmClock.EXTRA_MESSAGE,message) // Set the alarm message
            putExtra(AlarmClock.EXTRA_SKIP_UI, true) // Skip the alarm app's UI and go straight to saving the alarm
        }
        startActivity(alarmIntent)
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun listen_voice() {
        api.addMessage(true)
        this.state = ChatState.LISTENING
        speechRecognizer?.startListening(speechRecognizerIntent)

    }

    fun stop_listening() {
        this.state = ChatState.IDLE
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


}