package fr.xebia.hellogrpc

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import io.grpc.protobuf.services.ProtoReflectionService;

class PokedexServer {

    // The port on which the server should run
    private val port = 50052
    private var server: Server? = null
    private val pokemons = mutableListOf<Pokemon>()

    @Throws(IOException::class)
    private fun start() {
        // load data
        for (pokemon in Pokedex) {
            val values = pokemon.value.split(",")
            pokemons.add(
                Pokemon(
                    pokemon.key,
                    values[1].trim(),
                    values[0].trim(),
                    values[2].trim()
                )
            )
        }
        server = ServerBuilder.forPort(port)
            .addService(PokedexImpl(pokemons))
            .addService(ProtoReflectionService.newInstance())
            .build()
            .start()
        logger.log(Level.INFO, "Server started, listening on {0}", port)
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down")
                this@PokedexServer.stop()
                System.err.println("*** server shut down")
            }
        })
    }

    private fun stop() {
        server?.shutdown()
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    @Throws(InterruptedException::class)
    private fun blockUntilShutdown() {
        server?.awaitTermination()
    }

    internal class PokedexImpl(private val pokemons: List<Pokemon>) : PokedexGrpc.PokedexImplBase() {

        private val baseUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/"

        override fun getPokemon(request: PokedexRequest, responseObserver: StreamObserver<PokedexReply>) {
            val englishName = request.englishName
            var resultPokemon: Pokemon? = null
            logger.log(Level.INFO, englishName)
            for (pokemon in pokemons) {
                if (pokemon.englishName.toLowerCase() == englishName.toLowerCase()) {
                    resultPokemon = pokemon
                }
            }

            val reply = if (resultPokemon != null) {
                PokedexReply.newBuilder()
                    .setId(resultPokemon.id)
                    .setFrenchName(resultPokemon.frenchName)
                    .setType(resultPokemon.type)
                    .setImageUrl("$baseUrl${resultPokemon.id}.png")
                    .build()
            } else {
                // No pokémon is found return a null
                PokedexReply.newBuilder().build()
            }
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        }
    }

    companion object {
        private val logger = Logger.getLogger(HelloWorldServer::class.java.name)

        /**
         * Main launches the server from the command line.
         */
        @Throws(IOException::class, InterruptedException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val server = PokedexServer()
            server.start()
            server.blockUntilShutdown()
        }
    }
}