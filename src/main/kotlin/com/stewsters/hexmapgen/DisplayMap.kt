package com.stewsters.hexmapgen

import com.stewsters.hexmapgen.TerrainGenerator.generateHeight
import kaiju.math.getEuclideanDistance
import kaiju.noise.OpenSimplexNoise
import kaiju.pathfinder.Path
import kaiju.pathfinder.findGenericPath
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonOrientation
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridCalculator
import org.hexworks.mixite.core.api.HexagonalGridLayout
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PImage
import java.io.File
import kotlin.random.Random

class DisplayMap : PApplet() {

    private val widthTiles = 30
    private val heightTiles = 20
    private val radius = 40.0
    private lateinit var background: PGraphics
    private lateinit var grid: HexagonalGrid<TileData>
    private lateinit var calc: HexagonalGridCalculator<TileData>

    private val camera = Camera()
    private val cities = mutableListOf<Hexagon<TileData>>()
    private val roads = mutableSetOf<Pair<Hexagon<TileData>, Hexagon<TileData>>>()

    override fun settings() {
        size(1200, 800)

    }

    override fun setup() {
        frameRate(30f)

        val builder = HexagonalGridBuilder<TileData>()
        grid = builder
            .setGridWidth(widthTiles)
            .setGridHeight(heightTiles)
            .setGridLayout(HexagonalGridLayout.RECTANGULAR)
            .setOrientation(HexagonOrientation.FLAT_TOP)
            .setRadius(radius)
            .build()

        calc = builder.buildCalculatorFor(grid)


        // set initial camera pos
        camera.position.x = -widthTiles / 2f * radius.toFloat()
        camera.position.y = -heightTiles / 2f * radius.toFloat()

        background = createGraphics(width, height)
        background.beginDraw()
        background.background(200)
        background.endDraw()

        //  load all the images in
        TerrainType.URBAN.icons = loadImages("src/main/resources/MapParts/cities")
        TerrainType.TOWN.icons = loadImages("src/main/resources/MapParts/towns")
        TerrainType.HILL.icons = loadImages("src/main/resources/MapParts/hills")
        TerrainType.MOUNTAIN.icons = loadImages("src/main/resources/MapParts/mountains")
        TerrainType.FOREST.icons = loadImages("src/main/resources/MapParts/trees")
        TerrainType.FIELDS.icons = loadImages("src/main/resources/MapParts/fields")

        val n = Random.nextLong()

        // These shapes will resize bump the edges down
        val shapes = listOf({ x: Double, y: Double ->
            val d =
                getEuclideanDistance((widthTiles * radius) / 1.3, (heightTiles * radius) / 1.15, x, y)
            0.5 - 1 * (d / 800.0)
        })
        val osn = OpenSimplexNoise()

        grid.hexagons.forEach { hex ->

            val height = generateHeight(shapes, hex.centerX, hex.centerY, n)

            // Try to figure out the biome
            val terrainType =
                if (height < -0.25) {
                    TerrainType.DEEP_WATER //SHALLOW_WATER
                } else if (height < 0.4) {
                    val forest = osn.random2D(hex.centerX, hex.centerY)
                    if (forest > height)
                        TerrainType.FOREST
                    else
                        TerrainType.GRASSLAND

                } else if (height < 0.6) {
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

        // Build a city
        (0..5).forEach {
            findBestCityLocation()?.let { hex ->
                val d = hex.satelliteData.get()
                d.tileTitle = NameGen.city.random()
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
                .take(3)
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

    // Can build a city on this
    val cityTiles = listOf(TerrainType.FOREST, TerrainType.GRASSLAND)
    val waterTiles = listOf(TerrainType.DEEP_WATER, TerrainType.SHALLOW_WATER)
    val farmland = listOf(TerrainType.GRASSLAND)

    fun findBestCityLocation(): Hexagon<TileData>? {
        return grid.hexagons
            .filter {
                cityTiles.contains(it.satelliteData.get().type)
            }
            .filter {
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
                // Not near other cities
                score += cities.sumOf { otherCity -> calc.calculateDistanceBetween(potentialCity, otherCity) }

                score

            }
    }


    private fun loadImages(path: String): List<PImage> {
        return File(path)
            .listFiles()
            ?.map { loadImage(it.path) }
            .orEmpty()
    }

    override fun draw() {

        // Draw the background
        imageMode(PConstants.CORNER)
        image(background, 0f, 0f)

        push()
        // handle camera
        camera.draw(this)

        grid.hexagons.forEach { hex ->
            val satelliteData: TileData = hex.satelliteData.get()
            // Fill
            if (satelliteData.type?.color != null)
                fill(satelliteData.type!!.color)
            else
                noFill()

            // Draw the shape
            beginShape()
            hex.points.forEach {
                vertex(it.coordinateX.toFloat(), it.coordinateY.toFloat())
            }
            endShape(CLOSE)

            // Icon
            val icons = satelliteData.icons
            if (icons != null) {
                imageMode(CENTER)

                if (icons.size == 1) {
                    image(icons.first(), hex.centerX.toFloat(), hex.centerY.toFloat())
                } else {
                    // need a seeded location?
                    val cnt = icons.size
                    icons.forEachIndexed { index, it ->
                        val angle = 2 * PI / cnt * index
                        image(
                            it,
                            hex.centerX.toFloat() + radius.toFloat() / 2f * cos(angle),
                            hex.centerY.toFloat() + radius.toFloat() / 2f * sin(angle)
                        )
                    }

                }

            }

            fill(0xff000000.toInt())
            textAlign(CENTER)
            // Coordinates
            text(hex.cubeCoordinate.toCoord(), hex.centerX.toFloat(), hex.centerY.toFloat() + 30f)

            if (satelliteData.tileTitle != null) {
                text(satelliteData.tileTitle, hex.centerX.toFloat(), hex.centerY.toFloat() - 25f)
            }

        }

        // Draw Roads
        stroke(0x88DAA06D.toInt())
        strokeWeight(4f)

        roads.forEach {
            line(
                it.first.centerX.toFloat(), it.first.centerY.toFloat(),
                it.second.centerX.toFloat(), it.second.centerY.toFloat()
            )
        }
        stroke(1f)
        strokeWeight(1f)

        color(0)
        pop()
    }

    fun getPath(start: Hexagon<TileData>, end: Hexagon<TileData>): List<Hexagon<TileData>>? {
        val p = findGenericPath(
            cost = { x, y ->
                val set = listOf(x, y).sortedBy { it.gridX * widthTiles + it.gridZ }
                if (roads.contains(Pair(set.first(), set.last())))
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
                .map { calc.calculateDistanceBetween(hex, it).toDouble() }
                .minOrNull() ?: -10000.0

            return@Critter neighborDistance
        },
        Critter("Spiders") { hex: Hexagon<TileData> ->
            if (hex.satelliteData.get().type != TerrainType.FOREST)
                -10000.0
            else
                Random.nextDouble(1000.0) - (cities
                    .map { calc.calculateDistanceBetween(hex, it).toDouble() }
                    .minOrNull() ?: 10000.0)
        },
        Critter("Goblin") { hex: Hexagon<TileData> ->
            if (hex.satelliteData.get().type != TerrainType.HILL)
                -10000.0
            else
                Random.nextDouble(1000.0) - (cities
                    .map { calc.calculateDistanceBetween(hex, it).toDouble() }
                    .minOrNull() ?: 10000.0)
        }
    )

    // spider
    // goblin

}

class Critter(val name: String, val fitness: (Hexagon<TileData>) -> Double)

private fun <E> List<E>.randomList(i: Int): List<E> {
    return (1..i).map { this.random() }
}

private fun CubeCoordinate.toCoord(): String =
    "${gridX},${gridY},${gridZ}"

