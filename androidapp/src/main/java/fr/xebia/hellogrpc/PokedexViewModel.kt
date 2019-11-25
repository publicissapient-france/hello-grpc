package fr.xebia.hellogrpc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PokedexViewModel : ViewModel() {

    val pokemonLiveDate: LiveData<GetPokemonResult> get() = _pokemonLiveData
    private val _pokemonLiveData = MutableLiveData<GetPokemonResult>()

    fun getPokemon(host: String, portStr: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val port = if (portStr.isEmpty()) 0 else portStr.toInt()
                val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
                val stub = PokedexGrpc.newBlockingStub(channel)
                val request = PokedexRequest.newBuilder().setEnglishName(message.trim()).build()
                val reply = stub.getPokemon(request)
                _pokemonLiveData.postValue(GetPokemonResult.Success(reply))
                channel.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
                _pokemonLiveData.postValue(GetPokemonResult.Error(e))
            }
        }
    }
}

sealed class GetPokemonResult {
    data class Success(val reply: PokedexReply) : GetPokemonResult()
    data class Error(val exception: Exception) : GetPokemonResult()
}