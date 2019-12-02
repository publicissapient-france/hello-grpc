package fr.xebia.hellogrpc

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import io.grpc.StatusRuntimeException
import kotlinx.android.synthetic.main.activity_pokedex.*

class PokedexActivity : AppCompatActivity() {

    private val viewModel: PokedexViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pokedex)

        pokedex_response_name.movementMethod = ScrollingMovementMethod()
        viewModel.pokemonLiveDate.observe(this, Observer {
            onGetPokemon(it)
        })

        send_button.setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(pokemon_en_name_edit_text.windowToken, 0)
            send_button.isEnabled = false
            pokedex_response_name.text = ""

            viewModel.getPokemon(
                host_edit_text.text.toString(),
                port_edit_text.text.toString(),
                pokemon_en_name_edit_text.text.toString()
            )
        }
    }

    private fun onGetPokemon(result: GetPokemonResult) {
        send_button.isEnabled = true

        when (result) {
            is GetPokemonResult.Success -> {
                when {
                    result.reply.frenchName.isNotEmpty() -> {
                        question_mark.visibility = View.GONE
                        pokedex_response_name.visibility = View.VISIBLE
                        pokedex_response_type.visibility = View.VISIBLE
                        pokedex_response_image.visibility = View.VISIBLE

                        pokedex_response_name.text = result.reply.frenchName
                        pokedex_response_type.text = result.reply.type
                        Glide.with(this)
                            .load(result.reply.imageUrl)
                            .into(pokedex_response_image)
                    }
                    else -> {
                        resetUI()
                        Toast.makeText(this, R.string.no_result, Toast.LENGTH_LONG).show()
                    }
                }
            }
            is GetPokemonResult.Error -> {
                resetUI()
                when (result.exception) {
                    is StatusRuntimeException -> {
                        Toast.makeText(this, R.string.no_connection, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(this, R.string.error_response, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun resetUI() {
        question_mark.visibility = View.VISIBLE
        pokedex_response_name.visibility = View.GONE
        pokedex_response_type.visibility = View.GONE
        pokedex_response_image.visibility = View.GONE
    }
}
