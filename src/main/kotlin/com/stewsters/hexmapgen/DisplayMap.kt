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

        calc = builder.buildCalculatorFor(grid);


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

        val n = Random.nextLong()

        // These shapes will resize bump the edges down
        val shapes = listOf({ x: Double, y: Double ->
            val d =
                getEuclideanDistance((widthTiles * radius) / 1.3, (heightTiles * radius) / 1.15, x, y)
            0.5 - 1 * (d / 800.0)
        })
        val osn = OpenSimplexNoise()

        grid.hexagons.forEach { hex ->

//            val height = shapes.sumOf { it(hex.centerX, hex.centerY) }
            val height = generateHeight(shapes, hex.centerX, hex.centerY, n)

            // Try to figure out the biome
            val terrainType =
//                if (height < -0.50) {
//                TerrainType.DEEP_WATER
//            } else
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
                    icon = terrainType.icons.randomOrNull()
                )
            )
        }

        // Build a city
        (0..5).forEach {
            findBestCityLocation()?.let { hex ->
                val d = hex.satelliteData.get()
                d.tileTitle = "City"
                d.type = TerrainType.URBAN
                d.icon = TerrainType.URBAN.icons.randomOrNull()

                cities.add(hex)
            }
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
                // TODO: not near other cities
                score += cities
                    .map { otherCity -> calc.calculateDistanceBetween(potentialCity, otherCity) }
                    .sum ()


//                // contain farmland
//                if (neighbors.map { it.type }.any { farmland.contains(it) }) {
//                    score += 10
//                }

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
            if (satelliteData.icon != null) {
                imageMode(CENTER)
                image(satelliteData.icon, hex.centerX.toFloat(), hex.centerY.toFloat())
            }

            fill(0xff000000.toInt())
            textAlign(CENTER)
            // Coordinates
            text(hex.cubeCoordinate.toCoord(), hex.centerX.toFloat(), hex.centerY.toFloat() + 30f)

            if (satelliteData.tileTitle != null) {
                text(satelliteData.tileTitle, hex.centerX.toFloat(), hex.centerY.toFloat() - 25f)
            }

        }
        color(0)
        pop()
    }

    fun getPath(start: Hexagon<TileData>, end: Hexagon<TileData>): List<Hexagon<TileData>>? {
        val p = findGenericPath(
            cost = { x, y -> y.satelliteData.get().type?.traversalCost ?: 100.0 },
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

}

private fun CubeCoordinate.toCoord(): String =
    "${gridX},${gridY},${gridZ}"

