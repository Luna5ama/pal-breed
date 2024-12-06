package dev.luna5ama.palbreed

data class Pal(val species: Species, val sex: Sex?, val passive: Set<Passive>) {
    data class Passive(val name: String) {
        override fun toString(): String {
            return name
        }
    }
    data class Species(val id: Int, val name: String, val breedValue: Int, val indexOrder: Int) {
        override fun toString(): String {
            return name
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pal) return false

        if (species != other.species) return false
        if (sex != other.sex) return false
        if (passive != other.passive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = species.hashCode()
        result = 31 * result + (sex?.hashCode() ?: 0)
        result = 31 * result + passive.hashCode()
        return result
    }

    enum class Sex {
        MALE, FEMALE;

        companion object {
            fun matched(a: Sex?, b: Sex?): Boolean {
                if (a == null) return true
                if (b == null) return true
                return a != b
            }
        }
    }
}