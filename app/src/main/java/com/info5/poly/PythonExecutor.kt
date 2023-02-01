package com.info5.poly

import android.content.Context
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class PythonExecutor(context: Context) {
    private var python: Python
    private val sys: PyObject
    private val io: PyObject
    private val console: PyObject
    private val textOutputStream: PyObject

    init {
        if(!Python.isStarted()){
            Python.start(AndroidPlatform(context))
        }
        python = Python.getInstance()

        // 4. Obtain the system's input stream (available from Chaquopy)
        sys = python.getModule("sys")
        io = python.getModule("io")
        // Obtain the interpreter.py module
        console = python.getModule("interpreter")
        // 5. Redirect the system's output stream to the Python interpreter
        textOutputStream = io.callAttr("StringIO")
        sys["stdout"] = textOutputStream
    }

    fun fromString(code: String, callback: (String) -> Unit){
        GlobalScope.launch(Dispatchers.IO) {
            var interpreterOutput = String()
            try {
                console.callAttrThrows("mainTextCode", code)
                interpreterOutput = textOutputStream.callAttr("getvalue").toString()
            } catch (e: PyException) {
                interpreterOutput = e.message.toString()
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
            launch(Dispatchers.Main) {
                callback(interpreterOutput)
            }
        }
    }
}