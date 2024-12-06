package dev.luna5ama.palbreed

import java.util.*
import kotlin.collections.set
import kotlin.math.abs

class BreedCalculator(
    passiveData: List<Pal.Passive>,
    speciesData: List<Pal.Species>,
    specCombData: List<Triple<String, String, String>>
) {
    private val passiveNameMap: Map<String, Pal.Passive> = passiveData.associateBy { it.name }
    private val speciesNameMap: Map<String, Pal.Species> = speciesData.associateBy { it.name }

    private val lookupTable: Map<Pal.Species, Map<Pal.Species, Pal.Species>>

    init {
        val breedValueMap = TreeMap<Int, Pal.Species>()
        val indexOrderMap = TreeMap<Int, Pal.Species>()
        val specialResults = specCombData.map { it.third }.toSet()
        val specials = specCombData.groupBy {
            it.first
        }.mapValues { entry ->
            entry.value.associate {
                it.second to it.third
            }
        } + specCombData.groupBy {
            it.second
        }.mapValues { entry ->
            entry.value.associate {
                it.first to it.third
            }
        }
        speciesData.forEach {
            if (specialResults.contains(it.name)) return@forEach
            breedValueMap[it.breedValue] = it
            indexOrderMap[it.indexOrder] = it
        }

        val table = mutableMapOf<Pal.Species, MutableMap<Pal.Species, Pal.Species>>()
        for (a in speciesData) {
            val subTable = mutableMapOf<Pal.Species, Pal.Species>()
            val aSpec = specials[a.name]
            table[a] = subTable
            for (b in speciesData) {
                if (aSpec != null) {
                    val specChild = aSpec[b.name]
                    if (specChild != null) {
                        subTable[b] = speciesNameMap[specChild]!!
                        continue
                    }
                }
                val avgBreedValue = (a.breedValue + b.breedValue) / 2
                val above = breedValueMap.ceilingEntry(avgBreedValue)
                val below = breedValueMap.floorEntry(avgBreedValue)
                check(above != null || below != null) { "No species found" }
                val childSpecies = when {
                    above == null -> below.value
                    below == null -> above.value
                    else -> {
                        val diffAbove = above.key - avgBreedValue
                        val diffBelow = avgBreedValue - below.key
                        when {
                            diffAbove < diffBelow -> above.value
                            diffBelow < diffAbove -> below.value
                            below.value.indexOrder < above.value.indexOrder -> below.value
                            else -> above.value
                        }
                    }
                }
                subTable[b] = childSpecies
            }
        }
        lookupTable = table
    }

    fun getSpecies(name: String): Pal.Species {
        return speciesNameMap[name] ?: throw IllegalArgumentException("No species found")
    }

    fun getPassive(name: String): Pal.Passive {
        return passiveNameMap[name] ?: throw IllegalArgumentException("No passive found")
    }

    fun calc(a: Pal, b: Pal): Pal {
        val childSpecies =
            lookupTable[a.species]?.get(b.species) ?: throw IllegalArgumentException("No child species found")
        return Pal(childSpecies, null, a.passive + b.passive)
    }

    fun calcTree(pals: Set<Pal>, targetSpecies: Pal.Species, targetPassive: Set<Pal.Passive>, allowedPassive: Set<Pal.Passive>): BreedTreeNode {
        val checked = mutableSetOf<Pair<Pal, Pal>>()
        val poolSet = pals
            .filter { it.passive.isEmpty() || allowedPassive.containsAll(it.passive) }
            .map { BreedTreeNode(0, 0, it) }
            .toMutableSet()
        check(poolSet.isNotEmpty()) { "No input pal found" }

        val queue = PriorityQueue(compareBy<BreedTreeNode> {
            it.treeSize
        }.thenBy {
            abs(it.pal.species.breedValue - targetSpecies.breedValue)
        }.thenByDescending {
            it.pal.passive.intersect(targetPassive).size
        }.thenByDescending {
            it.pal.passive.intersect(allowedPassive).size
        })

        for (nodeA in poolSet) {
            for (nodeB in poolSet) {
                if (nodeA === nodeB) continue
                if (!Pal.Sex.matched(nodeA.pal.sex, nodeB.pal.sex)) continue
                if (!checked.add(nodeA.pal to nodeB.pal)) continue
                queue.add(BreedTreeNode(1, 1, calc(nodeA.pal, nodeB.pal), nodeA, nodeB))
            }
        }

        while (queue.isNotEmpty()) {
            val nodeA = queue.poll()
            if (!poolSet.add(nodeA)) continue

            val a = nodeA.pal

            if (a.species == targetSpecies && a.passive.containsAll(targetPassive)) {
                return nodeA
            }

            for (nodeB in poolSet) {
                val b = nodeB.pal
                if (nodeA === nodeB) continue
                if (!checked.add(a to b)) continue
                queue.add(BreedTreeNode(maxOf(nodeA.depth, nodeB.depth) + 1, nodeA.totalDepth + nodeA.totalDepth + 1, calc(nodeA.pal, nodeB.pal), nodeA, nodeB))
            }
        }

        throw IllegalStateException("No result found")
    }

    class BreedTreeNode(
        val depth: Int,
        val totalDepth: Long,
        val pal: Pal,
        val father: BreedTreeNode? = null,
        val mother: BreedTreeNode? = null
    ) {
        val treeSize: Int

        init {
            val queue = ArrayDeque<BreedTreeNode>()
            queue.add(this)
            var size = 0
            while (queue.isNotEmpty()) {
                val node = queue.poll()
                val father = node.father ?: continue
                val mother = node.mother ?: continue
                size++
                queue.add(father)
                queue.add(mother)
            }
            treeSize = size
        }

        fun pairTree(): List<Set<BreedResult>> {
            val result = mutableListOf<MutableSet<BreedResult>>()
            var layer = listOf(this)
            while (layer.isNotEmpty()) {
                result.add(layer.mapNotNullTo(mutableSetOf()) { node ->
                    val father = node.father ?: return@mapNotNullTo null
                    val mother = node.mother ?: return@mapNotNullTo null
                    BreedResult(father, mother, node)
                })
                layer = layer.flatMap { listOfNotNull(it.father, it.mother) }
            }
            result.dropLast(1).forEach { results ->
                results.removeIf {
                    if (it.father.father == null && it.father.mother == null
                        && it.mother.father == null && it.mother.mother == null) {
                        result.last().add(it)
                        true
                    } else {
                        false
                    }
                }
            }
            result.removeIf { it.isEmpty() }
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BreedTreeNode) return false

            if (pal != other.pal) return false

            return true
        }

        override fun hashCode(): Int {
            return pal.hashCode()
        }
    }

    data class BreedResult(val father: BreedTreeNode, val mother: BreedTreeNode, val child: BreedTreeNode)
}