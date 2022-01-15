import DataStructures.DFA
import DataStructures.NFA
import DataStructures.RegExp
import DataStructures.State

fun main(){
    // ((a'{25}[}wedf*]f|t{89}
    // "\\(\\(a\\|\\b\\)\\*'\\{25\\}\\)"
    // "\\(b\\{3\\}\\|a\\*\\)p" // "(b{3}|a*)p"
    // "[]abc*(d|e){2}\\(\\1[rof\\(]" // "abc*(d|e){2}"
    val s = "(abc)*"//"([as#d]|a*)p\\1"//"l#{3}k"//"([as#d]|a*)p\\1" // "(b{3}|a*)p"   "([asd]|a*)p"

    // testing //////////////
    //mins()                 //
    //intersection()         //
    //regexp_intersection()  //
    findMatches()          //
    /////////////////////////

//    val nfa = NFA(s)
//    nfa.treeToNFA()
//    nfa.minimize()
//    val dfa = DFA(s)
//    dfa.treeToDFA()
//    val re = dfa.recoverKpath()
    //var complement = dfa.buildComplement()
    //dfa.minimize()
    println("End")
}

fun findMatches() {
    val re = RegExp("aa*bb*")
    re.compile()
    val matches = re.findall("abaabbabbb")
}
fun regexp_intersection() {
    val re = RegExp("aa*bb*")
    re.compile()
    val s = "ab*"
    val dfa = DFA(s)
    val dfa1 = re.complement()
    val dfa2 = re.intersection(s)
    val dfa3 = re.intersection(dfa)
}

fun complement() {
    val s = "([as#d]|a*)p\\1"
    val dfa = DFA(s)
    val complement = dfa.buildComplement()
    println("END")
}

fun intersection() {
    val s1 = "aa*bb*"
    val s2 = "ab*"
    val dfa1 = DFA(s1)
    val dfa2 = DFA(s2)
    val intersection = dfa1.intersection(dfa2)
    println("END")
}

fun mins() {
    var state0 = State(0)
    state0.isError = true
    state0.addPositions(mutableListOf(0))
    var state1 = State(1)
    state1.addPositions(mutableListOf(1))
    var state2 = State(2)
    state2.addPositions(mutableListOf(2))
    state2.receiving = true
    var state3 = State(3)
    state3.addPositions(mutableListOf(3))
    state3.receiving = true
    var state4 = State(4)
    state4.addPositions(mutableListOf(4))
    state4.receiving = true

    state0.addTransition('l', state0)
    state0.addTransition('d', state0)

    state1.addTransition('l', state2)
    state1.addTransition('d', state0)

    state2.addTransition('l', state3)
    state2.addTransition('d', state4)

    state3.addTransition('l', state3)
    state3.addTransition('d', state4)

    state4.addTransition('l', state3)
    state4.addTransition('d', state4)

    var dfa = DFA("lll")
    dfa.addState(state0)
    dfa.addState(state1)
    dfa.addState(state2)
    dfa.addState(state3)
    dfa.addState(state4)
    dfa.setAlph(mutableListOf('l', 'd'))
    var p_fp: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    p_fp[1] = mutableListOf(2)
    p_fp[2] = mutableListOf(3, 4)
    p_fp[3] = mutableListOf(3, 4)
    p_fp[4] = mutableListOf(3, 4)
    dfa.setP_FP(p_fp)
    dfa.minimize()
}
