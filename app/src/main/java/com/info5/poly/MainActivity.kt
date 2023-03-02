package com.info5.poly

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
                    if(getPackageFromAppName(appName) != "") {
                        val packageName  = getPackageFromAppName(appName)
                        val i: Intent? = packageManager.getLaunchIntentForPackage(packageName)
                        if (i != null) {
                            applicationContext.startActivity(i);
                        }
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // TODO Auto-generated catch block
                }


        }
        fun getPackageFromAppName(appName: String): String {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val allApps = packageManager.queryIntentActivities(intent, 0)
            for (resolveInfo in allApps) {
                val label =
                    resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager).toString()
                if (label.contains(appName, ignoreCase = true) || appName.contains(label, ignoreCase = true)) {
                    return resolveInfo.activityInfo.packageName
                }
            }
            return ""
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