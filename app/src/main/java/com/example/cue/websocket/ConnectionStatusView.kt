package com.example.cue.websocket

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.cue.R
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConnectionStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusDot: ImageView
    private val statusText: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(32, 8, 32, 8)

        LayoutInflater.from(context).inflate(R.layout.view_connection_status, this, true)

        statusDot = findViewById(R.id.statusDot)
        statusText = findViewById(R.id.statusText)

        setBackgroundResource(android.R.color.background_dark)
    }

    fun bindToConnectionState(
        lifecycleOwner: LifecycleOwner,
        connectionState: StateFlow<ConnectionState>,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            connectionState.collect { state ->
                updateStatus(state)
            }
        }
    }

    private fun updateStatus(state: ConnectionState) {
        val (color, text) = when (state) {
            is ConnectionState.Connected -> {
                Color.GREEN to "Connected"
            }

            is ConnectionState.Connecting -> {
                Color.parseColor("#FFA500") to "Connecting..."
            }

            is ConnectionState.Disconnected -> {
                Color.RED to "Disconnected"
            }

            is ConnectionState.Error -> {
                val errorText = when (state.error) {
                    is ConnectionError.InvalidURL -> "Error: Invalid URL"
                    is ConnectionError.ConnectionFailed ->
                        "Error: Connection Failed (${state.error.message})"

                    is ConnectionError.ReceiveFailed ->
                        "Error: Receive Failed (${state.error.message})"
                }
                Color.RED to errorText
            }
        }

        statusDot.setColorFilter(color)
        statusText.setTextColor(color)
        statusText.text = text
    }
}
