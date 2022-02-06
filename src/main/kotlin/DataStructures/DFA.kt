package DataStructures

class DFA(rawString: String = "") {
    private var sTree: STree = STree(rawString)
    private var p_fp: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    private var pos = 1
    private var clearTokens: MutableList<Node?> = mutableListOf(null)
    private var alphabet: MutableList<Char?> = mutableListOf()
    var states: MutableList<State> = mutableListOf()
    private var min_states: MutableList<State> = mutableListOf()
    // error_pos = 0
    var stateNum = 0
    var start_state: State? = null
    private var last_states: MutableList<State> = mutableListOf()
    var current_state: State? = null

    var captureGroups: MutableList<DFA> = mutableListOf()

    // for testing ///////////////////////////////////////////////
    fun addState(state: State) {                                //
        states.add(state)                                       //
    }                                                           //
    fun setAlph(alphabet: MutableList<Char?>) {                 //
        this.alphabet = alphabet                                //
    }                                                           //
    fun setP_FP(p_fp: MutableMap<Int, MutableList<Int>>) {      //
        this.p_fp = p_fp                                        //
    }                                                           //
    //////////////////////////////////////////////////////////////

    init {
        if (rawString.isNotEmpty()) {
            this.treeToDFA()
        }
    }

    fun changeCurrState(letter: Char?): State? {
        var isExist = false
        findLoop@for (transition in this.current_state!!.transitions) {
            if (transition.first == letter) {
                isExist = true
                this.current_state = transition.second
                break@findLoop
            }
        }
        return if (isExist) this.current_state else null
    }

    fun minimize(): DFA {
        //  создаем P-разбиение принимающих и не принимающих состояний
        var P = mutableListOf<MutableList<Int>>(mutableListOf(), mutableListOf())
        for (state in states) {
            if (state.receiving) {
                P[1].add(state.n)
            } else {
                P[0].add(state.n)
            }
        }

        //  продолжаем разбиение
        var flag = true
        var size = P.size
        while (flag) {      // flag == true до тех пор, пока разбиение изменяется
            addedGroup@for (group_ind in 0 until P.size) {     //  проходим по каждой группе разбиения
//                for (state_ind in 1 until P[group_ind].size) {      // сравниваем каждое состояние с первым 'канонным'
                var state_ind = 1
                while (state_ind < P[group_ind].size) {
                    var isSame = true
                    checkState@for (symb in alphabet) {    // проверка идёт для каждого символа
                        // находим в какое состояние ведёт по заданному символу переход из первого ('канонного')
                        // состояния, на которое мы ориентируемся для нахождения идентичных
                        var nextState_0: Int = 0    //  номер следующего состояния для первого состояния
                        loop0@for (transition in states[P[group_ind][0]].transitions) {   // проходим по всем переходам
                            if (transition.first == symb) {
                                nextState_0 = transition.second.n
                                break@loop0
                            }
                        }
                        // находим в какое состояние ведёт по заданному символу переход из текущего состояния
                        var nextState_k: Int = 0    //  номер следующего состояния для текущего состояния
                        loopk@for (transition in states[P[group_ind][state_ind]].transitions) {
                            if (transition.first == symb) {
                                nextState_k = transition.second.n
                                break@loopk
                            }
                        }
                        // проверяем принадлежат ли два следующих состояния одной группе
                        for (groups in P) {
                            if ((nextState_0 in groups) xor (nextState_k in groups)) {
                                isSame = false
                                break@checkState
                            }
                        }
                    }
                    if (!isSame) {  // если не принадлежат одной группе, то надо добавить текущее состояние в группу
                                    // неидентичных (или создать её)
                        // удаляем неидентичное по переходам состояние и запоминаем его
                        val st = P[group_ind].removeAt(state_ind)
                        // если до этого не встречалось неидентичных, то создаем новую подгруппу
                        if (P.size == size) {
                            P.add(group_ind+1, mutableListOf(st))
                        } else {    // иначе добавляем в уже существующую текущее состояние
                            P[group_ind+1].add(st)
                        }
                    } else ++state_ind
                    if (P[group_ind].size == state_ind) break@addedGroup
                }
            }
            if (size == P.size) {   //  если группа разбиения не изменилась после обработки -> конец алгоритма
                flag = false
            } else size = P.size    // изменилась -> повторяем ещё раз
        }

        //  имеем новое разбиение -> для него надо построить минимизированный автомат
        if (P.size == states.size) {    // если ничего не изменилось, то минимальный ДКА:= данный ДКА
            min_states = states
        } else{     // если изменилось - перестраиваем
            for (group in P) {
//            min_states.add(State(P.indexOf(group)))
                if (0 in group) {   // если ошибочная позиция есть в группе (ErrorState обычно на нулевой позиции)
                    min_states.add(0, State(P.indexOf(group)))
                    min_states[0].isError = true
                    for (n in group) {
                        min_states[0].addPositions(states[n].positions)
                    }
                } else if (1 in group) {
                    var st = State(P.indexOf(group))
                    st.addPositions(states[1].positions)
                    min_states.add(1, st)
                } else {
                    var b = true
                    var positions = mutableSetOf<Int>()
                    for (stPos in group) {
                        if (states[stPos].receiving && b) {
                            min_states.add(State(P.indexOf(group), receiving = true))
                            b = false
                        }
                        positions.addAll(states[stPos].positions)
                    }
                    if (b) {
                        min_states.add(State(P.indexOf(group)))
                    }
                    min_states.last().positions = positions.toMutableList()
                }
            }
            // построение переходов в минимальном ДКА
            for (state in min_states) {
                if (state.isError) {
                    for (letter in alphabet) {
                        state.addTransition(letter, state)
                    }
                } else {
//                    for (stPos in state.positions) {
//                        for (nextPos in p_fp[stPos]!!) {
//                            for (nextState in min_states) {
//                                if (nextPos in nextState.positions) {
//                                    for ()
//                                    var tran = Pair(clearTokens[stPos]!!.symbol[0], nextState)
//                                    if (tran !in state.transitions) state.transitions.add(tran)
//                                }
//                            }
//                        }
//                    }
                    for (DFAstate in states) {
                        for (transition in DFAstate.transitions) {
                            for (nextState in min_states) {
                                if (DFAstate.positions.contains(state.positions) && transition.second.positions.contains(nextState.positions)) {
                                    var tr = Pair(transition.first, nextState)
                                    if (tr !in state.transitions) state.addTransition(tr.first, tr.second)
                                }
                            }
                        }
                    }
                }
            }
        }

        val dfa = DFA()

        // поиск start_position
        for (st in min_states) {
            if (st.positions.contains(this.start_state!!.positions)) {
                dfa.start_state = st
            }
        }

        // поиск last_positions
        for (st in min_states) {
            if (st.receiving) {
                dfa.last_states.add(st)
            }
        }

        dfa.alphabet = this.alphabet
        for (minState in this.min_states) {
            dfa.addState(minState)
        }
        return dfa
    }

    fun treeToDFA() {
        //builds position -> following positions
        treePreparation(sTree.tree!!)
        for (i in 1 until pos) {
            if (i !in p_fp) {
                p_fp[i] = mutableListOf()
            }
        }
        p_fp[pos-1] = mutableListOf()

        // builds DFA
        // добаляем всевозможные состояния
        //  надо добавить error состояние: pos = 0
        states.add(State(stateNum++))
        states.last().isError = true

        //  надо добавить стартовые состояния: pos = 1
        states.add(State(stateNum++))
        states.last().addPositions(sTree.tree!!.first)
        start_state = states[1]
        //  добавление состояний и переходов
        process(states[1])
    }

    private fun process(state: State) {
        if (!state.processed) {     //  проверяем обработано ли состояние
            if (state.isError) {    //  если состояние ошибочное, то просто добавляем все переходы в само себя
                for (letter in alphabet) {
                    state.addTransition(letter, state)
                }
            } else {
                for (letter in alphabet) {
                    var nextPos: MutableSet<Int> = mutableSetOf()   // множество следующих возможных состояний
                    for (currPos in state.positions) {
                        if ((letter == clearTokens[currPos]!!.symbol[0]) || ((letter == null) && (clearTokens[currPos]) is EmptyLeaf)) {
                            nextPos.addAll(p_fp[currPos]!!)
                        }
                    }
                    if (nextPos.isNotEmpty()) {     // если есть переходы по данной букве
                        var isExist = false
                        loop@ for (eachState in states) {   // проверка есть ли уже состояние с такой группой
                            if (nextPos == eachState.positions.toSet()) {
                                isExist = true
                                state.addTransition(letter, eachState)  // если есть, то добавляем переход в это состояние
                                break@loop
                            }
                        }
                        if (!isExist) {     // если нет, то создаём такое состояние, а потом переходим в него
                            states.add(State(stateNum++))
                            states.last().addPositions(nextPos.toMutableList())
                            state.addTransition(letter, states.last())
                        }
                    } else {
                        state.addTransition(letter, states[0])      // если нет состояний для перехода, то переходим в error-состояние
                    }
                }
            }

            isReceiving@for (position in sTree.tree!!.last) {
                if (position in state.positions) {
                    state.receiving = true
                    last_states.add(state)
                    break@isReceiving
                }
            }
            state.processed = true

        }
        for ((_, nextState) in state.transitions) {     // обрабатываем состояния, в которые можно перейти из текущего
            if (!nextState.processed) process(nextState)
        }


//        // добавление стартовых состояний
//        if ((state.n == 0) && !state.processed) {
//            for (first_pos in sTree.tree!!.first) {
//                for (thisState in states) {
//                    if ((first_pos in thisState.positions) && (thisState.positions.size == 1)) {
//                        state.addTransition(null, thisState)
//                    }
//                }
//            }
//            state.processed = true
//        } else if((state.isError) && !state.processed) {
//            // добавление переходов состояния - ошибки
//            for (letter in alphabet) {
//                state.addTransition(letter, state)
//            }
//            state.processed = true
//        } else if (!state.processed){
//            // добавление переходов промежуточным состояниям
//            for (letter in alphabet) {
//                for (position in state.positions) {
//                    if ((letter == clearTokens[position]!!.symbol[0]) || ((letter == null) && (clearTokens[position]) is EmptyLeaf)) {
////                        for (nextpos in p_fp[position]!!) {
////                            for (nextstate in states) {
////                                if (nextpos in nextstate.positions) {
////                                    state.addTransition(letter, nextstate)
////                                }
////                            }
////                        }
//                        for (nextstate in states) {
//                            if ((p_fp[position] == nextstate.positions) && (Pair(letter, nextstate) !in state.transitions)) {
//                                state.addTransition(letter, nextstate)
//                            }
//                        }
//                    } else {
//                        state.addTransition(letter, states[errorState])
//                    }
//                }
//            }
//            state.processed = true
//        }
//        for (next in state.transitions) {
//            if (!next.second.processed) process(next.second)
//        }
    }

    private fun treePreparation(node: Node) { //positions, nullable, first, last
        if (node.children.isNotEmpty()) {
            for (each in node.children) {
                treePreparation(each)
            }
        }
        if (node is Leaf || node is EmptyLeaf) {
            clearTokens.add(node)
            if ((node is Leaf) && (node.symbol[0] !in alphabet)) {
                alphabet.add(node.symbol[0])
            } else if ((node is EmptyLeaf) && (null !in alphabet)) {
                alphabet.add(null)
            }
            node.position = pos++
            if (node is EmptyLeaf) node.nullable = true
            node.first.add(node.position)
            node.last.add(node.position)
        } else if (node is Or || node is Set) {
            for (each in node.children) {
                node.nullable = node.nullable || each.nullable
                node.first.addAll(each.first)
                node.last.addAll(each.last)
            }
        } else if (node is Repeat || node is CaptureGroup) {
            for (each in node.children) {
                node.nullable = each.nullable
                node.first.addAll(each.first)
                node.last.addAll(each.last)
            }
        } else if (node is KleeneСlosure) {
            for (each in node.children) {
                node.nullable = true
                node.first.addAll(each.first)
                node.last.addAll(each.last)
            }
            for (last in node.last) {
                if (last !in p_fp) {
                    p_fp[last] = node.first
                } else {
                    p_fp[last]!!.addAll(node.first)
                }
            }
        } else if (node is Concat) {
            node.nullable = node.children[0].nullable && node.children[1].nullable
            if (node.children[0].nullable) {
                node.first.addAll(node.children[0].first)
                node.first.addAll(node.children[1].first)
            } else {
                node.first.addAll(node.children[0].first)
            }
            if (node.children[1].nullable) {
                node.last.addAll(node.children[0].last)
                node.last.addAll(node.children[1].last)
            } else {
                node.last.addAll(node.children[1].last)
            }
//            node.first.addAll(node.children[0].first)
//            node.last.addAll(node.children[1].last)

            for (last in node.children[0].last) {
                if (last !in p_fp) {
                    p_fp[last] = node.children[1].first
                } else {
                    p_fp[last]!!.addAll(node.children[1].first)
                }
            }
        }
    }

    // построение дополнения к языку
    fun buildComplement(alphabet: MutableList<Char> = mutableListOf()): DFA{
        val dfa = DFA()

        // построение нового алфивита
        var alphSet: MutableSet<Char?> = mutableSetOf()
        alphSet.addAll(this.alphabet)
        alphSet.addAll(alphabet)
        dfa.alphabet = alphSet.toMutableList()

        var complementStates = mutableListOf<State>()

        // запоминаем error-state
        var errorState: State? = null
        // запоминаем start-state
        var startState: State? = null

        // создание состояний нового автомата-дополнения
        for (oldState in this.states) {
            val newState = State(oldState.n, receiving = oldState.receiving.xor(true))
            newState.addPositions(oldState.positions.toMutableList())
            complementStates.add(newState)
            if (oldState.isError) errorState = newState
            if (oldState == this.start_state) startState = newState
        }
        // добавление переходов в новый автомат
        for (stateNum in this.states.indices) {
//            for (oldTransition in this.states[stateNum].transitions) {
//                complementStates[stateNum].addTransition(oldTransition.first, complementStates[oldTransition.second.n])
//            }
            for (letter in alphSet) {
                for (oldTransition in this.states[stateNum].transitions) {
                    // надо проверить есть ли переход с такой буквой
                    // если есть, то добавляем, если нет, то добавляем переход в ранее-error-state
                    if (letter == oldTransition.first)
                        complementStates[stateNum].addTransition(oldTransition.first, complementStates[oldTransition.second.n])
                    else {
                        if (errorState != null) {
                            complementStates[stateNum].addTransition(letter, errorState)
                        }
                    }
                }
            }
        }
        dfa.states = complementStates
        dfa.start_state = startState
        return dfa
    }

    // построение пересечения языков
    fun intersection(other: DFA): DFA {
        val dfa = DFA()

        var set: MutableSet<Char?> = mutableSetOf()
        set.addAll(this.alphabet)
        set.addAll(other.alphabet)

        dfa.alphabet = set.toMutableList()
        //  создание состояний нового автомата
        var intersectionStates = mutableListOf<State>()
        var n = 0
        for (firstState in this.states) {
            for (secondState in other.states) {
                val newState = State(n++, receiving = firstState.receiving and secondState.receiving)
                newState.addPositions(mutableListOf(firstState.n, secondState.n))
                newState.isError = firstState.isError or secondState.isError    // если хоть одно из состояний ошибочное,
                                                                                // то новое состояние - ошибочное
                intersectionStates.add(newState)
            }
        }
        // добавление переходов
        // напоминание: (нулевые состояния в данных автоматах (this,  other) - всегда ошибочные)
        for (newState in intersectionStates) {
            if ((newState.positions.first() == 1) and (newState.positions.last() == 1)) {
                dfa.start_state = newState
            }
            if (!newState.processed){
                if (newState.isError) {
                    // если состояние ошибочное, то добавляем переходы в само себя
                    for (symbol in set) {
                        newState.addTransition(symbol, newState)
                    }
                } else {
                    // надо найти для определённого символа следующие позиции p_ q_
                    //
                    // т.е. берём текущие позиции, ищем их в двух автоматах и для перехода с нужной буквой
                    // запоминаем следующие позиции
                    // проходим по новым состояниям и ищем нужное
                    for (symbol in set) {
                        var q = 0   // 0 - номер ошибочного состояния, так что если нет перехода по текущему символу,
                        var p = 0   // то он будет в состояние error
                        // поиск по первому автомату
                        firstLoop@for (firstState in this.states) {
                            if (newState.positions.first() == firstState.n) { // находим соответствующее состояние из старого автомата
                                for (transition in firstState.transitions) {
                                    if (symbol == transition.first) {
                                        q = transition.second.n
                                        break@firstLoop
                                    }
                                }
                            }
                        }
                        // поиск по второму автомату
                        secondLoop@for (firstState in other.states) {
                            if (newState.positions.last() == firstState.n) { // находим соответствующее состояние из старого автомата
                                for (transition in firstState.transitions) {
                                    if (symbol == transition.first) {
                                        p = transition.second.n
                                        break@secondLoop
                                    }
                                }
                            }
                        }
                        // ищем нужное следующее состояние в новом автомате
                        searchLoop@for (nextState in intersectionStates) {
                            if ((nextState.positions.first() == q) && (nextState.positions.last() == p)) {
                                newState.addTransition(symbol, nextState)
                                break@searchLoop
                            }
                        }
                    }
                    newState.processed = true
                }
            }
        }
        // находим достижимые состояния
        dfa.start_state?.let { findReachable(it) }
        // если какие-то состояния не достижимы - их можно удалить
        var i = 0
        while (i < intersectionStates.size) {
            if (!intersectionStates[i].isReachable) {
                intersectionStates.removeAt(i)
            } else {
                intersectionStates[i].n = i++
            }
        }

        // новые позиции для каждого state
        i = 0
        for (state in intersectionStates) {
            state.positions.clear()
            state.positions.add(i++)
        }
        dfa.states = intersectionStates

        return dfa
    }

    fun findReachable(state: State) {
        if (!state.isReachable){
            state.isReachable = true
            for (transition in state.transitions) {
                findReachable(transition.second)
            }
        }
    }

    fun recoverKpath(): String {
        val k = this.states.size
        var k_vec: Array<Array<Array<String?>>> = Array(k) { Array(k) { Array(k) { null } } }
        // заполнение базиса (при k = 0)
        for (state in this.states) {
            if (state.n != 0) {
                for (transition in state.transitions) {
                    if (transition.second.n != 0) {
                        k_vec[0]!![state.n][transition.second.n] = transition.first.toString()
                    }
                }
            }
        }
        for (i in 1 until k) {
            for (j in 1 until k) {
                if (k_vec[0]!![i][j].isNullOrBlank()) {
                    if (i == j) {
                        k_vec[0]!![i][j] = "#"
                    }
                }
            }
        }

        // заполнение уровней 1..k (k - число состояний)
        for (level in 1 until k) {
            for (i in 1 until k) {
                for (j in 1 until k) {
                    val r1 = k_vec[level-1][i][j]
                    val r2 = k_vec[level-1][i][level]
                    val r3 = k_vec[level-1][level][level]
                    val r4 = k_vec[level-1][level][j]
                    if ((r1 == null) && (null in mutableListOf(r2, r3, r4))) {
                        k_vec[level][i][j] = null
                    } else if (r1 == null) {
                        k_vec[level][i][j] = "$r2($r3)*$r4"
                    } else if (null in mutableListOf(r2, r3, r4)){
                        k_vec[level][i][j] = r1
                    } else {
                        if ((r2 == r3) && (r3 == r4)) k_vec[level][i][j] = "$r1|($r3)*"
                        else if (r2 == r3) k_vec[level][i][j] = "$r1|(($r3)*$r4)"
                        else if (r3 == r4) k_vec[level][i][j] = "$r1|($r2($r3)*)"
                        else k_vec[level][i][j] = "$r1|($r2($r3)*$r4)"
                    }
                }
            }
        }
        var regexp = ""
        for (last_state in this.last_states) {
            var str = k_vec[k-1][this.start_state!!.n][last_state.n]
            if (str != null){
                if (start_state == last_state) {
                    k_vec[k - 1][this.start_state!!.n][last_state.n] = "(${str})*"
                }
            regexp += "${k_vec[k - 1][this.start_state!!.n][last_state.n]}|"
            }
        }
        return regexp.dropLast(1)
    }

}