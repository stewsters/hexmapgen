package com.stewsters.hexmapgen.hex

import processing.core.PApplet
import kotlin.math.sqrt

public class HexGrid(
    var cols: Int,
    var rows: Int,
    radius: Float
) {
    //2D Matrix of Hexagon Objects
    var grid: Array<Array<Hexagon?>> = Array(cols) { arrayOfNulls(rows) }

    //Class Constructor required the grid size and cell radius
    init {
        // Let's assign the initial x,y coordinates outside the loop
        var x = sqrt(3f) * radius
        var y = radius

        //These two nested for loops will cycle all the columns in each row
        //and calculate the coordinates for the hexagon cells, generating the
        //class object and storing it in the 2D array.
        for (i in 0..<rows) {
            for (j in 0..<cols) {
                grid[j][i] = Hexagon(x.toFloat(), y.toFloat(), radius)
                x = (x + radius * sqrt(3f))//Calculate the x offset for the next column
            }

            y = (y + (radius * 3f) / 2f) //Calculate the y offset for the next row
            x = if ((i + 1) % 2 == 0)
                    (sqrt(3f) * radius)
            else
                    (radius * sqrt(3f) / 2f)
        }
    }

    //This function will redraw the entire table by calling the draw on each
    //hexagonal cell object
    fun draw(cntx: PApplet) {
        for (i in 0..<rows) {
            for (j in 0..<cols) {
                grid[j][i]!!.draw(cntx)
            }
        }
    }

    //This function will return the hexagonal cell object given its column and row
    fun getHex(col: Int, row: Int): Hexagon? {
        return grid[col][row]
    }
}