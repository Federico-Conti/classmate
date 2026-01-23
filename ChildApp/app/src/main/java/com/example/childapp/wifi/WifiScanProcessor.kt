package com.example.childapp.wifi

import android.util.Log
import kotlin.math.round

class WifiScanProcessor(
    private val windowSize: Int,
    private val thresholdDbm: Int,
    private val stableDecisionsNeeded: Int,
) {
    enum class State { OUT, IN }

    private val samples = mutableListOf<Int>()
    private var state: State = State.OUT
    private var previous: State = State.OUT
    private var stableCount: Int = 0

    fun onNewRssiSample(levelDbm: Int): String? {
        Log.d("WifiScanProcessor", "stableCount: $stableCount")
        samples.add(levelDbm)

        if (samples.size < windowSize) return null
        else if (samples.size > windowSize) {
            repeat(windowSize / 3) { if (samples.isNotEmpty()) samples.removeAt(0) }
            return null
        }

        val avg = weightedAverage(samples)
        Log.d("WifiScanProcessor", "AVG: $avg")
        previous = state
        state = if (avg > thresholdDbm) State.IN else State.OUT

        stableCount = if (state != previous) 0 else stableCount + 1

        repeat(windowSize / 3) { if (samples.isNotEmpty()) samples.removeAt(0) }

        if (stableCount == stableDecisionsNeeded) {
            stableCount = 0
            return if (state == State.IN) "entry" else "exit"
        }

        return null
    }

    private fun weightedAverage(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0

        var sum = 0.0
        var denom = 0.0
        for (i in values.indices) {
            val w = 1.0 / (values.size - i) // most recent higher values
            denom += w
            sum += values[i] * w
        }
        val res = sum / denom
        return round(res * 100) / 100.0
    }
}
