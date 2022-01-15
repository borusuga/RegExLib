package DataStructures

class State(var n: Int = 0, var processed: Boolean = false, var receiving: Boolean = false) {
    var isError: Boolean = false
    var isReachable = false
    var positions: MutableList<Int> = mutableListOf()
    var transitions: MutableList<Pair<Char?, State>> = mutableListOf()

    fun addTransition(char: Char?, nextState: State) {
        transitions.add(Pair(char, nextState))
    }

    fun addPositions(fpos: MutableList<Int>) {
        positions.addAll(fpos)
    }

    fun equals(other: State): Boolean {
        if (this.isError != other.isError) return false
        for (each in this.positions) {
            if (each !in other.positions) return false
        }
        return true
    }
}