package dev.luna5ama.palbreed

import java.net.URL

object DataInput {
    fun readCSV(url: URL): List<List<String>> {
        return url.readText()
            .lineSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { it.split(",") }
            .toList()
    }

    fun parseSpecies(data: List<List<String>>): List<Pal.Species> {
        return data.map {
            Pal.Species(it[0].toInt(), it[2], it[3].toInt(), it[4].toInt())
        }
    }

    fun parsePassive(data: List<List<String>>): List<Pal.Passive> {
        return data.map { Pal.Passive(it[2]) }
    }

    fun parseSpecialCombinations(data: List<List<String>>): List<Triple<String, String, String>> {
        return data.map {
            Triple(it[1], it[3], it[5])
        }
    }
}