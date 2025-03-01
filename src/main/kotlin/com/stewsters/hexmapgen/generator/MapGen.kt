package com.stewsters.hexmapgen.generator

import com.stewsters.hexmapgen.TileData
import com.stewsters.hexmapgen.generator.TerrainGenerator.generateHeightCelly
import com.stewsters.hexmapgen.map.HexMap
import com.stewsters.hexmapgen.map.cityTiles
import com.stewsters.hexmapgen.map.farmland
import com.stewsters.hexmapgen.map.waterTiles
import com.stewsters.hexmapgen.types.TerrainType
import kaiju.math.getEuclideanDistance
import kaiju.noise.OpenSimplexNoise
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.max
import kotlin.random.Random

fun generate(hexMap: HexMap) {
    val n = Random.nextLong()
    val totalWidth = hexMap.widthTiles * hexMap.radius
    val totalHeight = hexMap.heightTiles * hexMap.radius

    val northernMountainsShape = { x: Double, y: Double ->
        0.6 - (y / (totalWidth))
    }

    val islandShape = { x: Double, y: Double ->
        // This builds an island in the center
        val d =
            getEuclideanDistance((totalWidth) / 1.3, (totalHeight) / 1.15, x, y)

        0.5 - 1 * (d / (totalWidth / 1.5))
    }

    val bowlShape = { x: Double, y: Double ->
        // This builds an island in the center
        val d =
            getEuclideanDistance((totalWidth) / 1.3, (totalHeight) / 1.15, x, y)
//            println(widthTiles*radius)
        -0.5 + 1 * (d / (totalWidth / 1.5))
    }

    // These shapes will resize bump the edges down
    val shapes = listOf<(Double, Double) -> Double>(
        bowlShape
    )

    val osn = OpenSimplexNoise(n)

    val mountain = mutableListOf<Hexagon<TileData>>()
    hexMap.grid.hexagons.forEach { hex ->

//            val height = generateHeightPeaky(shapes, hex.centerX, hex.centerY, n)
        var height = generateHeightCelly(shapes, hex.centerX, hex.centerY, osn)

        // Try to figure out the biome
        val terrainType =
            if (hexMap.grid.getNeighborsOf(hex).size != 6) {
                height = max(0.7, height) // Set it to mountain height at least
                TerrainType.MOUNTAIN // Edge should be mountain
            } else if (height < 0.25) {
                TerrainType.DEEP_WATER //SHALLOW_WATER
            } else if (height < 0.6) {
                val forest = osn.random2D(hex.centerX, hex.centerY)
                if (forest > height)
                    TerrainType.FOREST
                else
                    TerrainType.GRASSLAND

            } else if (height < 0.7) {
                TerrainType.HILL
            } else {
                mountain.add(hex)
                TerrainType.MOUNTAIN
            }
        // assign biome

        hex.setSatelliteData(
            TileData(
                type = terrainType,
                elevation = height,
                icons = if (terrainType.multiIcon)
                    terrainType.icons.randomList((2..5).random())
                else if (terrainType.icons.isEmpty())
                    null
                else
                    listOf(terrainType.icons.random())
            )
        )
    }


    mountain.shuffled().take(10).forEach { spring ->
        // Rivers - start at springs, go downhill to form a pool
        var start = spring

        while (true) {
            val lowest =
                hexMap.grid.getNeighborsOf(start).minByOrNull { it.satelliteData.get().elevation ?: Double.MAX_VALUE }
            if (
                (lowest?.satelliteData?.get()?.elevation ?: Double.MAX_VALUE) < (start.satelliteData.get().elevation
                    ?: 0.0)
            ) {
                val link = Pair(start, lowest!!)
                hexMap.rivers.add(link)
                start = lowest
            } else {
                // we are at a local minima, throw a pond in
                val data = start.satelliteData.get()

                data.type = TerrainType.DEEP_WATER
                data.icons = listOf()
                break
            }
        }

    }

    // Build a city
    (0..10).forEach { _ ->
        findBestCityLocation(hexMap)?.let { hex ->
            val d = hex.satelliteData.get()
            d.tileTitle = NameGen.cityPrefix.random() + NameGen.citySuffix.random()
            d.type = TerrainType.URBAN
            d.icons = listOf(TerrainType.URBAN.icons.random())

            hexMap.cities.add(hex)
        }
    }

    // Expand farmland
    hexMap.cities.forEach { city ->
        hexMap.grid.getNeighborsOf(city)
            .filter { it.satelliteData.get().type == TerrainType.GRASSLAND }
            .shuffled()
            .take((1..3).random())
            .forEach { hex ->
                val data = hex.satelliteData.get()
                data.type = TerrainType.FIELDS
                data.icons =
                    TerrainType.FIELDS.icons.randomList((2..5).random())
            }
    }

    // connect each city up
    var lastCity: Hexagon<TileData>? = null
    hexMap.cities.forEach { cityHex ->
        lastCity?.let {
            val path = hexMap.getPath(it, cityHex)
            var lastHex: Hexagon<TileData>? = null
            path?.forEach { hex ->
                if (lastHex != null) {
                    val g = listOf(lastHex, hex).sortedBy { it.gridX * hexMap.widthTiles + it.gridZ }
                    hexMap.roads.add(Pair(g.first(), g[1]))
                }
                lastHex = hex
            }
        }
        lastCity = cityHex
    }

    // Critter powers
//    hexMap.critters.forEach { critter ->
//        grid.hexagons
//            .maxByOrNull { critter.fitness(it) }
//            ?.let { it.satelliteData.get().tileTitle = critter.name }
//    }
}


//    // seats of power for each creature
//    val critters = listOf(
//        Critter("Dragon") { hex: Hexagon<TileData> ->
//            // Dragon chooses the most inaccessible mountain areas
//            if (hex.satelliteData.get().type != TerrainType.MOUNTAIN)
//                return@Critter -10000.0
//
//            val neighborDistance: Double = cities
//                .minOfOrNull { calc.calculateDistanceBetween(hex, it).toDouble() }
//                ?: -10000.0
//
//            return@Critter neighborDistance
//        },
//        Critter("Spiders") { hex: Hexagon<TileData> ->
//            if (hex.satelliteData.get().type != TerrainType.FOREST)
//                -10000.0
//            else
//                Random.nextDouble(1000.0) - (cities
//                    .minOfOrNull { calc.calculateDistanceBetween(hex, it).toDouble() }
//                    ?: 10000.0)
//        },
//        Critter("Goblin") { hex: Hexagon<TileData> ->
//            if (hex.satelliteData.get().type != TerrainType.HILL)
//                -10000.0
//            else
//                Random.nextDouble(1000.0) - (cities
//                    .minOfOrNull { calc.calculateDistanceBetween(hex, it).toDouble() }
//                    ?: 10000.0)
//        }
//    )

fun findBestCityLocation(hexMap: HexMap): Hexagon<TileData>? {
    return hexMap.grid.hexagons
        .filter { // Filters for only the tiletypes that can hold a city
            cityTiles.contains(it.satelliteData.get().type)
        }
        .filter {// no edge cities
            hexMap.grid.getNeighborsOf(it).size == 6
        }
        .maxByOrNull { potentialCity ->
            val neighbors = hexMap.grid.getNeighborsOf(potentialCity)
                .toList()
                .map { it.satelliteData.get() }
            var score = 0.0

            // contain water
            if (neighbors.map { it.type }.any { waterTiles.contains(it) }) {
                score += 10
            }
            // contain farmland
            var landScore = 10.0
            neighbors.map { it.type }
                .filter { farmland.contains(it) }
                .forEach { _ ->
                    score += landScore
                    landScore /= 2
                }
            score += landScore

            // Not near other cities
            val adjacencyScore = hexMap.cities.minOfOrNull { otherCity ->
                hexMap.calc.calculateDistanceBetween(potentialCity, otherCity)
            } ?: 0
            score += adjacencyScore

            score

        }
}

fun <E> List<E>.randomList(i: Int): List<E> {
    return (1..i).map { this.random() }
}