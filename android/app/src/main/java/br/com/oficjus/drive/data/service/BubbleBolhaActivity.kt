package br.com.oficjus.drive.data.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class BubbleBolhaActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            layoutInflater.inflate(
                resources.getIdentifier("bolha_flutuante_layout", "layout", packageName),
                null
            )
        )

        val distancia = intent.getStringExtra(EXTRA_DISTANCIA) ?: "🚗 ---"
        val parada = intent.getStringExtra(EXTRA_PARADA) ?: "1/1"
        val podeAvancar = intent.getBooleanExtra(EXTRA_PODE_AVANCAR, false)
        val podeVoltar = intent.getBooleanExtra(EXTRA_PODE_VOLTAR, false)

        findViewById<TextView>(
            resources.getIdentifier("bf_distancia", "id", packageName)
        )?.text = distancia

        findViewById<TextView>(
            resources.getIdentifier("bf_parada", "id", packageName)
        )?.text = parada

        findViewById<Button>(
            resources.getIdentifier("bf_avancar", "id", packageName)
        )?.apply {
            alpha = if (podeAvancar) 1.0f else 0.4f
            setOnClickListener {
                sendBroadcast(Intent(ACAO_AVANCAR))
            }
        }

        findViewById<Button>(
            resources.getIdentifier("bf_voltar", "id", packageName)
        )?.apply {
            alpha = if (podeVoltar) 1.0f else 0.4f
            setOnClickListener {
                sendBroadcast(Intent(ACAO_VOLTAR))
            }
        }

        findViewById<Button>(
            resources.getIdentifier("bf_lista", "id", packageName)
        )?.setOnClickListener {
            sendBroadcast(Intent(ACAO_LISTA))
        }
    }

    companion object {
        const val ACAO_AVANCAR = "br.com.oficjus.drive.BUBBLE_AVANCAR"
        const val ACAO_VOLTAR = "br.com.oficjus.drive.BUBBLE_VOLTAR"
        const val ACAO_LISTA = "br.com.oficjus.drive.BUBBLE_LISTA"
        const val EXTRA_DISTANCIA = "distancia"
        const val EXTRA_PARADA = "parada"
        const val EXTRA_PODE_AVANCAR = "podeAvancar"
        const val EXTRA_PODE_VOLTAR = "podeVoltar"
    }
}