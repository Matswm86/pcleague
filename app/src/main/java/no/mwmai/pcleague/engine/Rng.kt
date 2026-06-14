package no.mwmai.pcleague.engine

import kotlin.math.ln
import kotlin.math.sqrt

/** Small deterministic PRNG (xorshift64) so a save reproduces exactly. */
class Rng(seed: Long) {
    private var s: Long = if (seed == 0L) 0x9E3779B97F4A7C15uL.toLong() else seed

    fun nextLong(): Long {
        var x = s
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        s = x
        return x
    }

    val state: Long get() = s

    fun nextDouble(): Double = ((nextLong() ushr 11).toDouble()) / (1L shl 53).toDouble()

    /** uniform int in [from, until). */
    fun int(from: Int, until: Int): Int {
        if (until <= from) return from
        val r = (nextLong() ushr 1) % (until - from).toLong()
        return from + r.toInt()
    }

    fun chance(p: Double): Boolean = nextDouble() < p

    fun <T> pick(list: List<T>): T = list[int(0, list.size)]

    /** Poisson sample via Knuth, clamped for safety. */
    fun poisson(lambda: Double): Int {
        if (lambda <= 0.0) return 0
        val l = kotlin.math.exp(-lambda)
        var k = 0
        var p = 1.0
        do {
            k++
            p *= nextDouble()
        } while (p > l && k < 30)
        return k - 1
    }

    fun gaussian(mean: Double, sd: Double): Double {
        val u1 = (nextDouble()).coerceIn(1e-9, 1.0)
        val u2 = nextDouble()
        val z = sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
        return mean + sd * z
    }
}
