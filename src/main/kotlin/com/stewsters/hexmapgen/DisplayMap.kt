package com.stewsters.hexmapgen

import com.stewsters.hexmapgen.TerrainGenerator.generateChunk
import org.hexworks.mixite.core.api.HexagonOrientation
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PImage
import java.awt.Color
import java.io.File
import kotlin.random.Random

class DisplayMap : PApplet() {

    lateinit var background: PGraphics

    //    lateinit var hexGrid: HexGrid
    lateinit var grid: HexagonalGrid<TileData>

//    camer

    override fun settings() {
        size(1200, 800)
    }

    override fun setup() {
        val builder: HexagonalGridBuilder<TileData> = HexagonalGridBuilder<TileData>()
            .setGridHeight(20)
            .setGridWidth(30)
            .setGridLayout(HexagonalGridLayout.RECTANGULAR)
            .setOrientation(HexagonOrientation.FLAT_TOP)
            .setRadius(30.0)
        grid = builder.build()

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

var n =        Random.nextLong()
        grid.hexagons.forEach { hex ->

            val height = generateChunk(listOf(), hex.centerX, hex.centerY, n)

            var icon: PImage? = null
            // Try to figure out the biome
            val terrainType = if (height < -0.50) {
                TerrainType.DEEP_WATER
            }else if (height< -0.25){
                TerrainType.SHALLOW_WATER

            } else if (height < 0.4) {
                TerrainType.FOREST
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

    fun loadImages(path: String): List<PImage> {
        return File(path)
            .listFiles()
            ?.map { loadImage(it.path) }
            .orEmpty()
    }

    override fun draw() {
        // Draw the background
        imageMode(PConstants.CORNER)
        image(background, 0f, 0f)

        grid.hexagons.forEach { hex ->
            val satelliteData: TileData = hex.satelliteData.get()
            // Fill
            if (satelliteData.type?.color != null)
                fill(satelliteData.type!!.color!!)
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
                imageMode(PApplet.CENTER)
                image(satelliteData.icon, hex.centerX.toFloat(), hex.centerY.toFloat())
            }
        }
        color(0)

    }

}