package com.stewsters.hexmapgen

import com.stewsters.hexmapgen.TerrainGenerator.generateHeight
import kaiju.math.getEuclideanDistance
import kaiju.noise.OpenSimplexNoise
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.HexagonOrientation
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
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

    private val camera = Camera()

    override fun settings() {
        size(1200, 800)

    }

    override fun setup() {
        frameRate(30f)

        val builder: HexagonalGridBuilder<TileData> = HexagonalGridBuilder<TileData>()
            .setGridHeight(heightTiles)
            .setGridWidth(widthTiles)
            .setGridLayout(HexagonalGridLayout.RECTANGULAR)
            .setOrientation(HexagonOrientation.FLAT_TOP)
            .setRadius(radius)
        grid = builder.build()

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
                text(satelliteData.tileTitle, hex.centerX.toFloat(), hex.centerY.toFloat() - 30f)
            }

        }
        color(0)
        pop()
    }

}

private fun CubeCoordinate.toCoord(): String =
    "${gridX},${gridY},${gridZ}"

