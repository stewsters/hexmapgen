package com.stewsters.hexmapgen

import processing.core.PImage
import java.awt.Color


enum class TerrainType(
    val color: Int = 0xFFFFFFFF.toInt(),
    val traversalCost: Double = 1.0
) {

    URBAN(0xFFEEEEEE.toInt()),
    TOWN(0xFFA52A2A.toInt()),

    // grasslands
    // marsh
    SHALLOW_WATER(0xFFadd8e7.toInt(), traversalCost = 30.0),
    DEEP_WATER(0xFF9dc8d7.toInt(), traversalCost = 50.0),
    GRASSLAND(0xFFbdd5a2.toInt(), traversalCost = 1.0),
    FIELDS(0xFFaaaa00.toInt(), traversalCost = 1.0 ),
    FOREST(0xff83a65a.toInt(), traversalCost = 2.0),
    HILL(0xff9fc5ad.toInt(), traversalCost = 10.0),
    MOUNTAIN(0xffb7b7b7.toInt(), traversalCost = 50.0);

    var icons: List<PImage> = listOf<PImage>()
}