package com.stewsters.hexmapgen

import org.hexworks.mixite.core.api.Hexagon

class Critter(val name: String, val fitness: (Hexagon<TileData>) -> Double)