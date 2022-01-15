package DataStructures

class NFA(rawString: String) {
    private var sTree: STree = STree(rawString)
    private var p_fp: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    private var pos = 1
    private var clearTokens: MutableList<Node?> = mutableListOf(null)
    private var alphabet: MutableList<Char?> = mutableListOf()
    private var states: MutableList<State> = mutableListOf()
    private var min_states: MutableList<State> = mutableListOf()
    var n = 0
    //var start: State? = null

    fun treeToNFA() {
        //builds position -> following positions
        treePreparation(sTree.tree!!)
        for (i in 1 until pos) {
            if (i !in p_fp) {
                p_fp[i] = mutableListOf()
            }
        }
        p_fp[pos-1] = mutableListOf()

        //builds NFA
        for (k in 0..pos) {
            states.add(State(k))
        }
        states[pos].isError = true
        process(states[0])
    }

    fun minimize() {
        // создаем массив принимающих и непринимающих состояний
        var K = mutableListOf<MutableList<Int>>()
        K.add(this.sTree.tree!!.last)
        for (state in states) {
            if ((state.n !in K[0]) && (state.n != 0)) {
                if (K.size == 1) {
                    K.add(mutableListOf(state.n))
                } else K[1].add(state.n)
            }
        }
        //
        var flag = true
        var size = K.size
        while (flag) {
            for (group_ind in 0 until K.size) {
                for (state_ind in 1 until K[group_ind].size) {
                    var isSame = true
                    for (symb in alphabet) {
                        // находим в какое состояние ведёт по заданному симолу переход из первого (канонного) состояния
                        var nextState_0: Int = 0
                        for (transition in states[K[group_ind][0]].transitions) {
                            if (transition.first == symb) {
                                nextState_0 = transition.second.n
                            }
                        }
                        // находим в какое состояние ведёт по заданному симолу переход из текущего состояния
                        var nextState_k: Int = 0
                        for (transition in states[K[group_ind][state_ind]].transitions) {
                            if (transition.first == symb) {
                                nextState_k = transition.second.n
                            }
                        }
                        // проверяем принадлежат ли два следующих состояния одной группе
                        for (groups in K) {
                            if ((nextState_0 in groups) xor (nextState_k in groups)) {
                                isSame = false
                            }
                        }
                    }
                    if (!isSame) {
                        // удаляем неидентичное по переходам состояние и запоминаем
                        val st = K[group_ind].removeAt(state_ind)
                        // если до этого не встречалось неидентичных, то создаем новую подгруппу
                        if (K.size == size) {
                            K.add(group_ind+1, mutableListOf(st))
                        } else {    // иначе добавляем в уже существующую новое состояние
                            K[group_ind+1].add(st)
                        }
                    }
                }
            }
            if (size == K.size) {
                flag = false
            } else size = K.size
        }

    }

    fun process(state: State) {
        if ((state.n == 0) && !state.processed) {
            for (each in sTree.tree!!.first) {
                state.addTransition(null, states[each])
            }
            state.processed = true
        } else if((state.n == pos) && !state.processed) {
            for (letter in alphabet) {
                state.addTransition(letter, state)
            }
            state.processed = true
        } else if (!state.processed){
            // по нулу не добавляем переходы
            // уже добавляем
            for (letter in alphabet) {
                if ((letter == clearTokens[state.n]!!.symbol[0]) || ((letter == null) && (clearTokens[state.n]) is EmptyLeaf)) {
                    for (each in p_fp.get(state.n)!!) {
                        state.addTransition(letter, states[each])
                    }
                } else {
                    state.addTransition(letter, states[pos])
                }
            }
            state.processed = true
        }
        for (next in state.transitions) {
            if (!next.second.processed) process(next.second)
        }
    }

    fun treePreparation(node: Node) { //positions, nullable, first, last
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

}