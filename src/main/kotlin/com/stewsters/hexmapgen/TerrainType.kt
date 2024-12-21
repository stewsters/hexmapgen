package com.stewsters.hexmapgen

import processing.core.PImage
import java.awt.Color


enum class TerrainType(val color: Int = 0xFFFFFFFF.toInt()) {
    URBAN(0xFFBBBBBB.toInt()),
    TOWN(0xFF99aa99.toInt()),

    // grasslands
    // marsh
    SHALLOW_WATER(0xFFB3EBF2.toInt()),
    DEEP_WATER(0xFF93aabd2.toInt()),
    GRASSLAND(0xFFe2F2cd.toInt()),
    FIELDS(0xFFaaaa00.toInt()),
    FOREST(0xff337744.toInt()),
    HILL(Color.LIGHT_GRAY.rgb),
    MOUNTAIN(Color.PINK.rgb);

    var icons: List<PImage> = listOf<PImage>()
}