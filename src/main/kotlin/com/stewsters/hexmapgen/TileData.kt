package com.stewsters.hexmapgen

import com.stewsters.hexmapgen.types.TerrainType
import org.hexworks.mixite.core.api.contract.SatelliteData
import processing.core.PImage

data class TileData(
    var type: TerrainType? = null,
    val elevation: Double? = null,
    var icons: List<PImage>? = null,
    var tileTitle: String? = null,

    override var movementCost: Double = 1.0,
    override var opaque: Boolean = false,
    override var passable: Boolean = true

) : SatelliteData