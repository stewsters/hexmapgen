package com.stewsters.hexmapgen.map

import com.stewsters.hexmapgen.TileData
import com.stewsters.hexmapgen.generator.NameGen
import com.stewsters.hexmapgen.generator.TerrainGenerator.generateHeightCelly
import com.stewsters.hexmapgen.types.Critter
import com.stewsters.hexmapgen.types.TerrainType
import kaiju.math.getEuclideanDistance
import kaiju.noise.OpenSimplexNoise
import kaiju.pathfinder.Path
import kaiju.pathfinder.findGenericPath
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridCalculator
import kotlin.random.Random

// Can build a city on this
val cityTiles = listOf(TerrainType.FOREST, TerrainType.GRASSLAND)
val waterTiles = listOf(TerrainType.DEEP_WATER, TerrainType.SHALLOW_WATER)
val farmland = listOf(TerrainType.GRASSLAND)


class HexMap(builder: HexagonalGridBuilder<TileData>) {
    val grid: HexagonalGrid<TileData> = builder.build()
    val calc: HexagonalGridCalculator<TileData> = builder.buildCalculatorFor(grid)

    private val widthTiles = builder.getGridWidth()
    private val heightTiles = builder.getGridHeight()
    private val radius = builder.getRadius()


    // part of the map
    val cities = mutableListOf<Hexagon<TileData>>()
    val roads = mutableSetOf<Pair<Hexagon<TileData>, Hexagon<TileData>>>()
    val rivers = mutableSetOf<Pair<Hexagon<TileData>, Hexagon<TileData>>>()


    fun generate() {
        val n = Random.nextLong()
        val totalWidth = widthTiles * radius
        val totalHeight = heightTiles * radius

        val northernMountainsShape = { x: Double, y: Double ->
            0.6 - (y / (totalWidth))
        }

        val islandShape = { x: Double, y: Double ->
            // This builds an island in the center
            val d =
                getEuclideanDistance((totalWidth) / 1.3, (totalHeight) / 1.15, x, y)
            println(widthTiles * radius)
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

        grid.hexagons.forEach { hex ->

//            val height = generateHeightPeaky(shapes, hex.centerX, hex.centerY, n)
            val height = generateHeightCelly(shapes, hex.centerX, hex.centerY, osn)

            // Try to figure out the biome
            val terrainType =
                if (grid.getNeighborsOf(hex).size != 6) {
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
                    TerrainType.MOUNTAIN
                }
            // assign biome

            hex.setSatelliteData(
                TileData(
                    type = terrainType,
                    icons = if (terrainType.multiIcon)
                        terrainType.icons.randomList((2..5).random())
                    else if (terrainType.icons.isEmpty())
                        null
                    else
                        listOf(terrainType.icons.random())
                )
            )
        }

        //TODO Rivers - start at springs, go downhill


        // Build a city
        (0..10).forEach { _ ->
            findBestCityLocation()?.let { hex ->
                val d = hex.satelliteData.get()
                d.tileTitle = NameGen.cityPrefix.random() + NameGen.citySuffix.random()
                d.type = TerrainType.URBAN
                d.icons = listOf(TerrainType.URBAN.icons.random())

                cities.add(hex)
            }
        }

        // Expand farmland
        cities.forEach { city ->
            grid.getNeighborsOf(city)
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
        cities.forEach { cityHex ->
            lastCity?.let {
                val path = getPath(it, cityHex)
                var lastHex: Hexagon<TileData>? = null
                path?.forEach { hex ->
                    if (lastHex != null) {
                        val g = listOf(lastHex!!, hex).sortedBy { it.gridX * widthTiles + it.gridZ }
                        roads.add(Pair(g.first(), g[1]))
                    }
                    lastHex = hex
                }
            }
            lastCity = cityHex
        }

        // Critter powers
        critters.forEach { critter ->
            grid.hexagons
                .maxByOrNull { critter.fitness(it) }
                ?.let { it.satelliteData.get().tileTitle = critter.name }
        }
    }


    fun findBestCityLocation(): Hexagon<TileData>? {
        return grid.hexagons
            .filter { // Filters for only the tiletypes that can hold a city
                cityTiles.contains(it.satelliteData.get().type)
            }
            .filter {// no edge cities
                grid.getNeighborsOf(it).size == 6
            }
            .maxByOrNull { potentialCity ->
                val neighbors = grid.getNeighborsOf(potentialCity)
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
                val adjacencyScore = cities.minOfOrNull { otherCity ->
                    calc.calculateDistanceBetween(potentialCity, otherCity)
                } ?: 0
                score += adjacencyScore

                score

            }
    }


    fun getPath(start: Hexagon<TileData>, end: Hexagon<TileData>): List<Hexagon<TileData>>? {
        val p = findGenericPath(
            cost = { x, y ->
                val set = listOf(x, y).sortedBy { it.gridX * widthTiles + it.gridZ }
                val key = Pair(set.first(), set.last())
                if (rivers.contains(key))
                    5.0
                if (roads.contains(key))
                    1.0
                else
                    y.satelliteData.get().type?.traversalCost ?: 100.0
            },
            heuristic = { s, t -> calc.calculateDistanceBetween(s, t).toDouble() },
            neighbors = { grid.getNeighborsOf(it).toList() },
            start = start,
            end = end,
        )

        return when (p) {
            is Path.Success -> p.data
            else -> null
        }
    }

    // seats of power for each creature
    val critters = listOf(
        Critter("Dragon") { hex: Hexagon<TileData> ->
            // Dragon chooses the most inaccessible mountain areas
            if (hex.satelliteData.get().type != TerrainType.MOUNTAIN)
                return@Critter -10000.0

            val neighborDistance: Double = cities
                .minOfOrNull { calc.calculateDistanceBetween(hex, it).toDouble() }
                ?: -10000.0

            return@Critter neighborDistance
        },
        Critter("Spiders") { hex: Hexagon<TileData> ->
            if (hex.satelliteData.get().type != TerrainType.FOREST)
                -10000.0
            else
                Random.nextDouble(1000.0) - (cities
                    .minOfOrNull { calc.calculateDistanceBetween(hex, it).toDouble() }
                    ?: 10000.0)
        },
        Critter("Goblin") { hex: Hexagon<TileData> ->
            if (hex.satelliteData.get().type != TerrainType.HILL)
                -10000.0
            else
                Random.nextDouble(1000.0) - (cities
                    .minOfOrNull { calc.calculateDistanceBetween(hex, it).toDouble() }
                    ?: 10000.0)
        }
    )

}

private fun <E> List<E>.randomList(i: Int): List<E> {
    return (1..i).map { this.random() }
}