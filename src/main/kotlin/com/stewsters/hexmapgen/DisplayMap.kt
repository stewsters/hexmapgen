package com.stewsters.hexmapgen

import com.stewsters.hexmapgen.hex.HexGrid
import com.stewsters.hexmapgen.hex.Hexagon
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PGraphics
import java.awt.Color
import java.io.File
import javax.imageio.ImageIO

class DisplayMap : PApplet() {

    lateinit var background: PGraphics
    lateinit var hexGrid: HexGrid

    var colors = listOf<Int>(
        0xFFB3EBF2.toInt(), // light blue
        0xFFcee2cd.toInt(), // light green
        Color.LIGHT_GRAY.rgb,  // light grey

    )

    override fun settings() {
        size(1200, 800)
    }

    override fun setup() {
        background = createGraphics(width, height)
        background.beginDraw()
        background.background(200)
        background.endDraw()

        hexGrid = HexGrid(30, 20, 40f)

        // TODO: load all the images in

        val trees = File("src/main/resources/MapParts/towns")
            .listFiles()
            ?.map { loadImage(it.path) }


        for(x in 0 until 30){
            for(y in 0 until 20){
                val hex = hexGrid.getHex(x,y)!!
                hex.c = colors.random()
                hex.icon = trees?.random()
            }
        }

    }

    override fun draw() {
        // Draw the background
        imageMode(PConstants.CORNER)
        image(background, 0f, 0f)

//        val hex = Hexagon(30f, 30f, 20f)
//        hex.draw(this)

        hexGrid.draw(this)
    }

}