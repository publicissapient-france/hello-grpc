package fr.xebia.hellogrpc

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.android.synthetic.main.activity_pokedex.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PokedexActivity : AppCompatActivity() {

    private var channel: ManagedChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pokedex)

        pokedex_response_name.movementMethod = ScrollingMovementMethod()

        send_button.setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(pokemon_en_name_edit_text.windowToken, 0)
            send_button.isEnabled = false
            pokedex_response_name.text = ""

            GlobalScope.launch {
                val result = getPokemon(
                    host_edit_text.text.toString(),
                    port_edit_text.text.toString(),
                    pokemon_en_name_edit_text.text.toString()
                )
                withContext(Dispatchers.Main) {
                    send_button.isEnabled = true
                    result?.let {
                        onMessageReceived(it.frenchName, it.type, it.imageUrl)
                    }
                }
            }
        }
    }

    private fun onMessageReceived(frenchName: String, type: String, imageUrl: String) {
        try {
            channel?.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        question_mark.visibility = View.GONE
        pokedex_response_name.visibility = View.VISIBLE
        pokedex_response_type.visibility = View.VISIBLE
        pokedex_response_image.visibility = View.VISIBLE

        pokedex_response_name.text = frenchName
        pokedex_response_type.text = type
        Glide.with(this)
            .load(imageUrl)
            .into(pokedex_response_image)
    }

    @WorkerThread
    suspend fun getPokemon(host: String, portStr: String, message: String): PokedexReply? {
        return withContext(Dispatchers.IO) {
            try {
                val port = if (portStr.isEmpty()) 0 else portStr.toInt()
                val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
                val stub = PokedexGrpc.newBlockingStub(channel)
                val request = PokedexRequest.newBuilder().setEnglishName(message.trim()).build()
                val reply = stub.getPokemon(request)
                reply
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
