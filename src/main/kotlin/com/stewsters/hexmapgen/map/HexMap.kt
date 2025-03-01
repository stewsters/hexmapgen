package com.stewsters.hexmapgen.map

import com.stewsters.hexmapgen.TileData
import com.stewsters.hexmapgen.types.TerrainType
import kaiju.pathfinder.Path
import kaiju.pathfinder.findGenericPath
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridCalculator

// Can build a city on this
val cityTiles = listOf(TerrainType.FOREST, TerrainType.GRASSLAND)
val waterTiles = listOf(TerrainType.DEEP_WATER, TerrainType.SHALLOW_WATER)
val farmland = listOf(TerrainType.GRASSLAND)


class HexMap(builder: HexagonalGridBuilder<TileData>) {
    val grid: HexagonalGrid<TileData> = builder.build()
    val calc: HexagonalGridCalculator<TileData> = builder.buildCalculatorFor(grid)

    val widthTiles = builder.getGridWidth()
    val heightTiles = builder.getGridHeight()
    val radius = builder.getRadius()

    // part of the map
    val cities = mutableListOf<Hexagon<TileData>>()
    val roads = mutableSetOf<Pair<Hexagon<TileData>, Hexagon<TileData>>>()
    val rivers = mutableSetOf<Pair<Hexagon<TileData>, Hexagon<TileData>>>()


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
}
