package com.stewsters.hexmapgen.generator

import kaiju.noise.OpenSimplexNoise
import kotlin.math.abs

object TerrainGenerator {

    fun generateHeightPeaky(
        shapeMods: List<(x: Double, y: Double) -> Double>,
        x: Double,
        y: Double,
        osn : OpenSimplexNoise
    ): Double {

        var ridginess = fbm(osn, x, y, 3, 1.0 / 200.0, 1.0, 2.0, 1.0)
        ridginess = abs(ridginess) * -1

        // Experimental elevation
        val elevation =
            fbm(osn, x, y, 3, 1.0 / 200.0, 1.0, 2.0, 0.3) * ridginess + shapeMods.sumOf { it(x, y) }

        // decent elevation
//        val elevation = max(
//            fbm(el, x, y, 3, 1.0 / 200.0, 1.0, 2.0, 0.3),
//            ridginess
//        ) + shapeMods.sumOf { it(x, y) }

        return elevation
    }

    fun generateHeightCelly(
        shapeMods: List<(x: Double, y: Double) -> Double>,
        x: Double,
        y: Double,
        osn : OpenSimplexNoise
    ): Double {

        var ridginess = fbm(osn, x, y, 3, 1.0 / 1000.0, 1.0, 2.0, 1.0)
        ridginess = (abs(ridginess) * -0.5) + 1

        val elevationNoise = 0.5 * fbm(osn, x, y, 3, 1.0 / 300.0, 1.0, 2.0, 0.3)

        return 0.5 * ridginess + 0.5 * elevationNoise + 0.5 * shapeMods.sumOf { it(x, y) }
    }


    fun fbm(
        osn: OpenSimplexNoise,
        x: Double,
        y: Double,
        octaves: Int,
        frequency: Double,
        amplitude: Double,
        lacunarity: Double,
        gain: Double
    ): Double {
        var freq = frequency
        var amp = amplitude
        var total = 0.0
        for (i in 0 until octaves) {
            total += osn.random2D(x * freq, y * freq) * amp
            freq *= lacunarity
            amp *= gain
        }
        return total
    }

}