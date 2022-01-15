package DataStructures

import javax.print.attribute.standard.MediaSize

class RegExp(var rawString: String) {
    lateinit var dfa: DFA

    fun compile(): DFA {
        dfa = DFA(this.rawString)
        return dfa.minimize()
    }

    // дополнение к языку
    fun complement(): DFA {
        return this.dfa.buildComplement()
    }

    // пересечение языков
    fun intersection(otherString: String): DFA {
        val otherDFA = DFA(otherString)
        return this.dfa.intersection(otherDFA)
    }

    fun intersection(otherDFA: DFA): DFA {
        return this.dfa.intersection(otherDFA)
    }

    fun findall(string: String): MutableList<String> {
        val modStr = "$string$"
        var matches: MutableList<String> = mutableListOf()
        var pos = 0
        while (pos < string.length) {
            this.dfa.current_ctate = this.dfa.start_state
            var currPos = 0
            var currStr = ""
            var previousReceiver = false
            substrLoop@for (letter in modStr.substring(pos)) {
                val tmpState = this.dfa.changeCurrState(letter)
                if ((tmpState == null) || (tmpState.isError)) {
                    if ((currStr.length > 0) and previousReceiver) {
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