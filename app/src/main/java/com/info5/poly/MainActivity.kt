package com.info5.poly

import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.info5.poly.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val JS_OBJ_NAME = "AndroidAPI"

    private inner class WebAndroidAPI {
        private var isRecording: Boolean = false;

        @JavascriptInterface
        fun setRecording(recording: Boolean){
            this.isRecording = recording;
            Log.d("Poly Debug", "recording: " + this.isRecording);
        }
    }

    private inner class WebAPI(private val webView: WebView) {
        private fun escapeJS(str: String): String{
            return str
                .replace("'", "\'")
                .replace("\n", "\\n");
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAndroidAPI(), JS_OBJ_NAME)
        webView.loadUrl("file:///android_asset/web/poly.html");

        webView.webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                Log.d("WebViewConsole", message!!)
            }
        }

    }
}