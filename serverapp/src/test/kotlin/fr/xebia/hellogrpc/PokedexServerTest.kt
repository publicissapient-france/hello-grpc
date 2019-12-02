package fr.xebia.hellogrpc

import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PokedexServerTest {

    @get:Rule
    val grpcCleanup = GrpcCleanupRule()

    /**
     * To test the server, make calls with a real stub using the in-process channel, and verify
     * behaviors or state changes from the client side.
     */
    @Test
    @Throws(Exception::class)
    fun pokedexImpl_getPokemon() {
        // Generate a unique in-process server name.
        val serverName = InProcessServerBuilder.generateName()

        // Create a server, add service, start, and register for automatic graceful shutdown.
        val pokemons = mutableListOf<Pokemon>()
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
        grpcCleanup.register(
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(PokedexServer.PokedexImpl(pokemons))
                .build()
                .start()
        )

        val blockingStub = PokedexGrpc.newBlockingStub(
            // Create a client channel and register for automatic graceful shutdown.
            grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor()
                    .build()
            )
        )

        val reply = blockingStub.getPokemon(PokedexRequest.newBuilder().setEnglishName("jigglypuff").build())

        assertEquals(39, reply.id)
        assertEquals("Rondoudou", reply.frenchName)
    }
}