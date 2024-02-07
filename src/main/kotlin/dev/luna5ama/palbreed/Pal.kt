package dev.luna5ama.palbreed

data class Pal(val species: Species, val sex: Sex?, val passive: Set<Passive>) {
    data class Passive(val name: String) {
        override fun toString(): String {
            return name
        }
    }
    data class Species(val name: String, val breedValue: Int, val indexOrder: Int) {
        override fun toString(): String {
            return name
        }
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