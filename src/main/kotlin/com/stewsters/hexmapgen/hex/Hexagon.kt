package com.stewsters.hexmapgen.hex

import processing.core.PApplet
import processing.core.PConstants.CLOSE
import processing.core.PConstants.TWO_PI
import processing.core.PImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class Hexagon(var centx: Float, var centy: Float, var radius: Float) {

    var angle: Float = TWO_PI / 6

    //    var fill: Boolean = false
    // Display info
    var c: Int? = null
    var icon: PImage? = null

    //The draw function will define the fill values and calculate the coordinates
    fun draw(cnxt: PApplet) {

        if (c != null)
            cnxt.fill(c!!)
        else cnxt.noFill()

        cnxt.beginShape()
        var a: Float = PI.toFloat() / 6f
        while (a < TWO_PI) {
            val sx: Float = centx + cos(a) * radius
            val sy: Float = centy + sin(a) * radius
            cnxt.vertex(sx, sy)
            a += angle
        }
        cnxt.endShape(CLOSE)
        if (icon != null) {
            cnxt.imageMode(PApplet.CENTER)
            cnxt.image(icon, centx, centy)
        }
    }

    //The following are all simply utility functions for setting parameters
    fun centx(): Float {
        return centx
    }

    fun centy(): Float {
        return centy
    }

    val colour: Int?
        get() = c

    fun setFillColour(col: Int?) {
        c = col
    }

//    fun setNoFill(yesno: Boolean) {
//        fill = null
//    }
}