package com.stewsters.hexmapgen.types

import com.stewsters.hexmapgen.TileData
import org.hexworks.mixite.core.api.Hexagon

class Critter(val name: String, val fitness: (Hexagon<TileData>) -> Double)