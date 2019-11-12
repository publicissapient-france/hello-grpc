package fr.xebia.hellogrpc

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        grpc_response_text!!.movementMethod = ScrollingMovementMethod()
        send_button!!.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(host_edit_text!!.windowToken, 0)
        send_button!!.isEnabled = false
        grpc_response_text!!.text = ""
        GrpcTask(this)
            .execute(
                host_edit_text!!.text.toString(),
                message_edit_text!!.text.toString(),
                port_edit_text!!.text.toString()
            )
    }

    private class GrpcTask constructor(activity: Activity) : AsyncTask<String, Void, String>() {
        private val activityReference: WeakReference<Activity> = WeakReference(activity)
        private var channel: ManagedChannel? = null

        override fun doInBackground(vararg params: String): String {
            val host = params[0]
            val message = params[1]
            val portStr = params[2]
            val port = if (TextUtils.isEmpty(portStr)) 0 else Integer.valueOf(portStr)
            return try {
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
                val stub = GreeterGrpc.newBlockingStub(channel)
                val request = HelloRequest.newBuilder().setName(message).build()
                val reply = stub.sayHello(request)
                reply.message
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()

                "Failed... : %s".format(sw)
            }
        }

        override fun onPostExecute(result: String) {
            try {
                channel?.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            val activity = activityReference.get() ?: return
            val resultText: TextView = activity.findViewById(R.id.grpc_response_text)
            val sendButton: Button = activity.findViewById(R.id.send_button)

            resultText.text = result
            sendButton.isEnabled = true
        }
    }
}
