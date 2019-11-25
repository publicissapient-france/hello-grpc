package fr.xebia.hellogrpc

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.inputmethod.InputMethodManager
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var channel: ManagedChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grpc_response_text.movementMethod = ScrollingMovementMethod()

        send_button.setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(host_edit_text.windowToken, 0)
            send_button.isEnabled = false
            grpc_response_text.text = ""

            GlobalScope.launch {
                val result = sendMessage(
                    host_edit_text.text.toString(),
                    message_edit_text.text.toString(),
                    port_edit_text.text.toString()
                )
                withContext(Dispatchers.Main) {
                    onMessageReceived(result)
                }
            }
        }
    }

    private fun onMessageReceived(result: String) {
        try {
            channel?.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        grpc_response_text.text = result
        send_button.isEnabled = true
    }

    @WorkerThread
    suspend fun sendMessage(host: String, message: String, portStr: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val port = if (portStr.isEmpty()) 0 else portStr.toInt()
                val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
                val stub = GreeterGrpc.newBlockingStub(channel)
                val request = HelloRequest.newBuilder().setName(message).build()
                val reply = stub.sayHello(request)
                reply.message
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()
                "Call failed: $sw"
            }
        }
    }
}
