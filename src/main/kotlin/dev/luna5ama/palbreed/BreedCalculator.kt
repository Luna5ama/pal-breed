package dev.luna5ama.palbreed

import java.util.TreeMap

class BreedCalculator(passiveData: List<Pal.Passive>, speciesData: List<Pal.Species>) {
    private val breedValueMap = TreeMap<Int, Pal.Species>()
    private val indexOrderMap = TreeMap<Int, Pal.Species>()
    private val passiveNameMap = passiveData.associateBy { it.name }
    private val speciesNameMap = speciesData.associateBy { it.name }

    init {
        speciesData.forEach {
            breedValueMap[it.breedValue] = it
            indexOrderMap[it.indexOrder] = it
        }
    }

    fun getSpecies(name: String): Pal.Species {
        return speciesNameMap[name] ?: throw IllegalArgumentException("No species found")
    }

    fun getPassive(name: String): Pal.Passive {
        return passiveNameMap[name] ?: throw IllegalArgumentException("No passive found")
    }

    private fun calc(a: Pal, b: Pal): List<Pal> {
        val avgBreedValue = (a.species.breedValue + b.species.breedValue) / 2
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
        return listOf(
            Pal(childSpecies, null, a.passive),
            Pal(childSpecies, null, b.passive),
            Pal(childSpecies, null, a.passive + b.passive)
        )
    }

    fun calcTree(pals: List<Pal>, targetSpecies: Pal.Species, targetPassive: List<Pal.Passive>): BreedTreeNode {
        val checked = mutableSetOf<Pair<Pal, Pal>>()
        val poolSet = pals
            .filter { targetPassive.containsAll(it.passive) }
            .map { BreedTreeNode(it) }
            .toMutableSet()
        check(poolSet.isNotEmpty()) { "No input pal found" }

        val queue = ArrayDeque<BreedTreeNode>()

        for (nodeA in poolSet) {
            for (nodeB in poolSet) {
                if (nodeA === nodeB) continue
                if (!Pal.Sex.matched(nodeA.pal.sex, nodeB.pal.sex)) continue
                if (!checked.add(nodeA.pal to nodeB.pal)) continue
                queue.add(BreedTreeNode(calc(nodeA.pal, nodeB.pal).last(), nodeA, nodeB))
            }
        }

        while (queue.isNotEmpty()) {
            val nodeA = queue.removeFirst()
            if (!poolSet.add(nodeA)) continue

            val a = nodeA.pal

            if (a.species == targetSpecies && a.passive.containsAll(targetPassive)) {
                return nodeA
            }

            for (nodeB in poolSet) {
                val b = nodeB.pal
                if (nodeA === nodeB) continue
                if (!checked.add(a to b)) continue
                queue.add(BreedTreeNode(calc(nodeA.pal, nodeB.pal).last(), nodeA, nodeB))
            }
        }

        throw IllegalStateException("No result found")
    }

    class BreedTreeNode(val pal: Pal, val father: BreedTreeNode? = null, val mother: BreedTreeNode? = null) {
        fun pairTree(): List<List<BreedResult>> {
            val result = mutableListOf<List<BreedResult>>()
            var layer = listOf(this)
            while (layer.isNotEmpty()) {
                result.add(layer.mapNotNull { node ->
                    val father = node.father ?: return@mapNotNull null
                    val mother = node.mother ?: return@mapNotNull null
                    BreedResult(father, mother, node)
                })
                layer = layer.flatMap { listOfNotNull(it.father, it.mother) }
            }
            return result

        }
    }

    class BreedResult(val father: BreedTreeNode, val mother: BreedTreeNode, val child: BreedTreeNode)
}