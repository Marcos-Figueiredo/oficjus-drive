package br.com.oficjus.drive.domain.usecase

import br.com.oficjus.drive.domain.Endereco
import kotlin.math.*

object OtimizarRotaUseCase {

    data class Posicao(val latitude: Double, val longitude: Double)

    /**
     * Otimiza a ordem das paradas usando Nearest-Neighbor TSP
     * com distância Haversine.
     */
    fun otimizar(
        paradas: List<Endereco>,
        posicaoAtual: Posicao? = null
    ): List<Endereco> {
        if (paradas.size <= 2) {
            // Com 2 ou menos, mantém ordem de digitação (referencia)
            // — evita desempate arbitrário em paradas da mesma rua
            return paradas
        }

        val comCoords = paradas.filter { it.temCoordenadas }
        val semCoords = paradas.filter { !it.temCoordenadas }

        if (comCoords.size < 2) return paradas

        // Ordena por referência original (ordem de digitação) antes de otimizar
        val ordenadasPorRef = comCoords.sortedBy { it.referencia }
        val coords = ordenadasPorRef.map { it.latitude!! to it.longitude!! }
        val indices = ordenadasPorRef.indices.toMutableList()
        val ordem = mutableListOf<Int>()

        // Começa pela primeira referência digitada, ou pela posição atual
        var ultimo = if (posicaoAtual != null) {
            posicaoAtual.latitude to posicaoAtual.longitude
        } else {
            coords[0]
        }

        while (indices.isNotEmpty()) {
            var melhor = -1
            var melhorDist = Double.MAX_VALUE

            for (i in indices.indices) {
                val dist = haversine(ultimo, coords[indices[i]])
                if (dist < melhorDist) {
                    melhorDist = dist
                    melhor = i
                }
            }

            val idx = indices.removeAt(melhor)
            ordem.add(idx)
            ultimo = coords[idx]
        }

        val resultado = ordem.map { ordenadasPorRef[it] } + semCoords
        return resultado.mapIndexed { index, endereco ->
            endereco.copy(ordem = index + 1)
        }
    }

    /**
     * Calcula a distância em metros entre dois pontos (Haversine)
     */
    fun haversine(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val R = 6371e3
        val dLat = (b.first - a.first) * PI / 180
        val dLng = (b.second - a.second) * PI / 180
        val aLat = a.first * PI / 180
        val bLat = b.first * PI / 180

        val x = sin(dLat / 2).pow(2) +
                cos(aLat) * cos(bLat) * sin(dLng / 2).pow(2)
        return R * 2 * atan2(sqrt(x), sqrt(1 - x))
    }
}