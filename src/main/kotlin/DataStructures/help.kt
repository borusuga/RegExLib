package DataStructures

fun MutableList<Int>.contains(other: MutableList<Int>): Boolean {
    for (each in this) {
        if (each in other)
            return true
    }
    return false
}