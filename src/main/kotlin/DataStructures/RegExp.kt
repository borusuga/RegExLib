package DataStructures

class RegExp(var rawString: String) {
    lateinit var dfa: DFA

    fun compile(): DFA {
        dfa = DFA(this.rawString)
        return dfa.minimize()
    }

    // дополнение к языку
    fun complement(alphabet: MutableList<Char> = mutableListOf()): DFA {
        return this.dfa.buildComplement(alphabet)
    }

    // пересечение языков
    fun intersection(otherString: String): DFA {
        val otherDFA = DFA(otherString)
        return this.dfa.intersection(otherDFA)
    }

    fun intersection(otherDFA: DFA): DFA {
        return this.dfa.intersection(otherDFA)
    }

    fun findAll(string: String): MutableList<String> {
        val modStr = "$string$"
        var matches: MutableList<String> = mutableListOf()
        var pos = 0
        if ((modStr.length == 1) && (this.dfa.start_state!!.receiving)) {
            return mutableListOf("")
        }
        while (pos < string.length) {
            this.dfa.current_state = this.dfa.start_state
            var currPos = 0
            var currStr = ""
            var previousReceiver = false
            substrLoop@for (letter in modStr.substring(pos)) {
                val tmpState = this.dfa.changeCurrState(letter)
                if ((tmpState == null) || (tmpState.isError) || (letter == '$')) {
                    if ((currStr.isNotEmpty()) and previousReceiver) {
                        matches.add(currStr)
                        pos += currPos
                    } else ++pos
                    break@substrLoop
                } else {
                    currStr += letter
                    if (tmpState.receiving) {
                        previousReceiver = true
                    }
                }
                ++currPos
            }
        }
        return matches
    }

}