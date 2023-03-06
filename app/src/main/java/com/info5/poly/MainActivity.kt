package com.info5.poly

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.provider.AlarmClock
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.info5.poly.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val JS_OBJ_NAME = "AndroidAPI"
    private lateinit var python: PythonExecutor

    private inner class WebAndroidAPI {
        private var isRecording: Boolean = false

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
        fun openApplication(appName:String){


                try {
                    val i: Intent? =packageManager.getLaunchIntentForPackage  (appName);
                    if (i!=null) {
                        applicationContext.startActivity(i);
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // TODO Auto-generated catch block
                }


        }
        @JavascriptInterface
        fun getMobileBrand(): String {
            val  manufacturer = Build.MANUFACTURER;
            val model = Build.MODEL;
            print(manufacturer.plus(model));
            return manufacturer.plus(model); }
        @JavascriptInterface
        fun getAndroidVersion() : String? {
            return Build.VERSION.RELEASE;
        }

        @JavascriptInterface
        fun sendSMS(phoneNumber:String, message:String){
            val uri = Uri.parse("smsto:".plus(phoneNumber))
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            startActivity(intent)
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
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


}