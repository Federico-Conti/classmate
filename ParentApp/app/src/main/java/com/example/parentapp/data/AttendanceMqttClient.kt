package com.example.parentapp.data

import android.os.Handler
import android.os.Looper
import com.example.parentapp.BuildConfig
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class ConfirmedAttendance(val eventId: String, val childId: String, val type: String, val time: Long)

class AttendanceMqttClient(private val onEvent: (ConfirmedAttendance) -> Unit) {
    val configurationError: String? = if (BuildConfig.MQTT_PASSWORD.isBlank()) {
        "Realtime is not configured. Set pocMqttPassword in user Gradle properties and rebuild."
    } else null

    private val lock = Any()
    private var client: MqttAsyncClient? = null
    private var active = false
    private var connecting = false
    private var selectedChildId: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val retry = Runnable { synchronized(lock) { connect() } }

    fun start() = synchronized(lock) {
        if (active) return@synchronized
        active = true
        if (configurationError != null) return@synchronized
        client ?: MqttAsyncClient(
            "tcp://${BuildConfig.POC_HOST}:1883",
            "parent-${UUID.randomUUID()}",
            MemoryPersistence()
        ).also { created ->
            created.setCallback(object : MqttCallbackExtended {
                override fun connectionLost(cause: Throwable?) = Unit
                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    synchronized(lock) {
                        connecting = false
                        if (active) selectedChildId?.let { created.subscribe(topic(it), 1) }
                        else if (created.isConnected) created.disconnect()
                    }
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val event = runCatching {
                        val json = JSONObject(String(message.payload, Charsets.UTF_8))
                        if (json.getInt("schemaVersion") != 1) return@runCatching null
                        val childId = json.getString("childId")
                        val type = json.getString("type")
                        if (!childId.matches(Regex("[A-Za-z0-9_-]{1,64}")) ||
                            (type != "entry" && type != "exit")) return@runCatching null
                        val eventId = json.getString("eventId")
                        UUID.fromString(eventId)
                        ConfirmedAttendance(
                            eventId, childId, type,
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
                                .parse(json.getString("occurredAt"))!!.time
                        )
                    }.getOrNull() ?: return
                    synchronized(lock) {
                        if (active && event.childId == selectedChildId && topic(event.childId) == topic) onEvent(event)
                    }
                }
            })
            client = created
        }
        connect()
    }

    private fun connect() {
        val mqtt = client ?: return
        if (active && !mqtt.isConnected && !connecting) {
            connecting = true
            val options = MqttConnectOptions().apply {
                userName = BuildConfig.MQTT_USERNAME
                password = BuildConfig.MQTT_PASSWORD.toCharArray()
                isAutomaticReconnect = true
                isCleanSession = true
            }
            runCatching {
                mqtt.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(token: IMqttToken?) = Unit
                    override fun onFailure(token: IMqttToken?, exception: Throwable?) {
                        synchronized(lock) {
                            connecting = false
                            if (active) handler.postDelayed(retry, 5_000)
                        }
                    }
                })
            }.onFailure {
                connecting = false
                handler.postDelayed(retry, 5_000)
            }
        }
    }

    fun selectChild(childId: String?) = synchronized(lock) {
        if (selectedChildId == childId) return@synchronized
        val previous = selectedChildId
        selectedChildId = childId
        val mqtt = client ?: return@synchronized
        if (!active || !mqtt.isConnected) return@synchronized
        if (previous == null) {
            childId?.let { mqtt.subscribe(topic(it), 1) }
        } else {
            mqtt.unsubscribe(topic(previous), null, object : IMqttActionListener {
                override fun onSuccess(token: IMqttToken?) = subscribeCurrent(mqtt, childId)
                override fun onFailure(token: IMqttToken?, exception: Throwable?) {
                    synchronized(lock) {
                        runCatching { mqtt.disconnectForcibly() }
                        connecting = false
                        if (active) handler.postDelayed(retry, 5_000)
                    }
                }
            })
        }
    }

    private fun subscribeCurrent(mqtt: MqttAsyncClient, childId: String?) = synchronized(lock) {
        if (active && mqtt.isConnected && childId != null && childId == selectedChildId) {
            mqtt.subscribe(topic(childId), 1)
        }
    }

    fun stop() = synchronized(lock) {
        active = false
        handler.removeCallbacks(retry)
        runCatching { client?.takeIf { it.isConnected }?.disconnect() }
    }

    fun close() = synchronized(lock) {
        stop()
        runCatching { client?.disconnectForcibly() }
        runCatching { client?.close() }
        client = null
    }

    private fun topic(childId: String) = "classmate/v1/children/$childId/events"
}
