package DataStructures

import kotlin.jvm.Throws
import java.util.ArrayDeque


class STree(private var rawString: String) {
    private var tokens: MutableList<Node> = mutableListOf()
    private lateinit var numberedGroups: MutableList<Node?>
    var tree: Node? = null
    init {
        if (rawString.isNotEmpty()) {
            numberedGroups = MutableList(rawString.countBr()+1) {null}
            this.buildTokens()
            this.buildTree()
        }
    }
//    fun buildTokens() {
//        var i = 0
//        while (i < rawString.length) {
//            when (rawString[i]) {
//                '(' -> {
//                    tokens.add(OpenPriority().createNode())
//                    ++i
//                }
//                ')' -> {
//                    tokens.add(ClosePriority().createNode())
//                    ++i
//                }
//                '\\' -> {
//                    if (i == rawString.length - 1) callException(0, i)
//                    tokens.add(Leaf(rawString[i+1].toString()).createNode())
//                    i+=2
//                }
//                '|' -> {
//                    if (i == 0 && i == rawString.length-1) callException(1, i)
//                    tokens.add(Or(rawString[i].toString()).createNode())
//                }
//                '*' -> {
//                    if (i > 0) tokens.add(KleeneClosure(rawString[i].toString()).createNode())
//                    else callException(2, i)
//                    ++i
//                }
//                '{' -> {
//                    val b = ('}' in rawString.substring(i+1))
//                    val nums = rawString.substring(i+1).substringBefore('}')
//                    if (nums.isDigit() && b) {
//                        tokens.add(Repeat("{$nums}").createNode())
//                        i+=nums.length+2
//                    } else callException(3, i)
//                }
//                '[' -> {
//                    val value = rawString.getSet(rawString, i)
//                    if (value == null) callException(4, i)
//                    else {
//                        tokens.add(Set(value).createNode()!!)
//                        i+=value.length+2
//                    }
//                }
//                !in meta -> {
//                    tokens.add(Leaf(rawString[i].toString()).createNode())
//                    i+=1
//                }
//                else -> ++i
//            }
//        }
//    }

    private fun buildTokens() {
        var i = 0
        rawString = "($rawString$)"
        while (i < rawString.length) {
            if (rawString[i] == '\\') {
                if (i == rawString.length - 1) callException(0, i)
                ++i
                if (rawString[i].isDigit()) {
                    var num = 0
                    while (rawString[i].isDigit()) {
                        num = num * 10 + rawString[i].digitToInt()
                        ++i
                    }
                    if (num < rawString.countBr()) {
                        val captureNode = CaptureGroup("numbered group").createNode()
                        captureNode.n = num
                        tokens.add(captureNode)
                    } else callException(4, i-1)
                } else {
                    tokens.add(Leaf(rawString[i].toString()).createNode())
                    ++i
                }
            } else if (rawString[i] !in meta) {
                tokens.add(Leaf(rawString[i].toString()).createNode())
                ++i
            } else {
                when (rawString[i]) {
                    '(' -> {
                        tokens.add(OpenPriority().createNode())
                        ++i
                    }
                    ')' -> {
                        tokens.add(ClosePriority().createNode())
                        ++i
                    }
                    '|' -> {
                        if (i == 0 && i == rawString.length-1) callException(1, i)
                        tokens.add(Or(rawString[i].toString()).createNode())
                        ++i
                    }
                    '*' -> {
                        if (i > 0) tokens.add(KleeneСlosure(rawString[i].toString()).createNode())
                        else callException(2, i)
                        ++i
                    }
                    '{' -> {
                        val b = ("}" in rawString.substring(i+1))
                        val nums = rawString.substring(i+1).substringBefore("}")
                        if (nums.isDigit() && b) {
                            tokens.add(Repeat("{$nums}").createNode())
                            i+=nums.length+2
                        } else callException(3, i)
                    }
                    '[' -> {
                        val value = rawString.getSet(i)
                        if (value == null) callException(4, i)
                        else {
                            tokens.add(Set(value).createNode()!!)
                            i+=value.length+2
                        }
                    }
                    '#' -> {
                        tokens.add(EmptyLeaf(rawString[i].toString()).createNode())
                        ++i
                    }
                    else -> {
                        callException(404, i)
                    }
                }
            }
        }
    }

    private fun buildTree() {
        val bracketStack = ArrayDeque<Pair<Int, Int>>() // <index, n_of_capture_group>
        var n = 0
        // bracket processing
        var i = 0
        val tokensCopy = tokens.toMutableList()
        while (i < tokensCopy.size) {
            if (tokensCopy[i].getType() == TreeNodes.OPEN_PRIORITY)
                bracketStack.push(Pair(i, n++))
            if (tokensCopy[i].getType() == TreeNodes.CLOSE_PRIORITY) {
                val captureTokens = tokensCopy.subList(bracketStack.first.first+1, i).toMutableList()
                val captureNode = CaptureGroup(mutableListOf(processCG(captureTokens)))
                captureNode.n = bracketStack.first.second
                for (j in bracketStack.first.first..i) {
                    tokensCopy.removeAt(bracketStack.first.first)
                }
                i = bracketStack.first.first
                tokensCopy.add(i, captureNode)
                numberedGroups[captureNode.n] = captureNode
                bracketStack.pop()
            }
            ++i
        }

        tree = tokensCopy[0]
        replaceNGtoCG(tree!!)
        transformRepeats(tree!!)
    }

    private fun processCG(captureTokens: MutableList<Node>): Node {
        var i = 0
        while (i < captureTokens.size) {    // processing KLEENE_CLOSURE & REPEAT
            when (captureTokens[i].getType()) {
                TreeNodes.KLEENE_CLOSURE -> {
                    if (captureTokens[i].children.size > 0) {
                        ++i
                    } else {
                        captureTokens[i].addChild(captureTokens.subList(i - 1, i))
                        captureTokens.removeAt(i - 1)
                    }
                }
                TreeNodes.REPEAT -> {
                    if (captureTokens[i].children.size > 0) {
                        ++i
                    } else {
                        captureTokens[i].addChild(captureTokens.subList(i - 1, i))
                        captureTokens.removeAt(i - 1)
                    }
                }
                else -> {
                    ++i
                }
            }
        }
        i = 0
        while (i < captureTokens.size) {    // processing OR
            if (captureTokens[i].getType() == TreeNodes.OR) {
                captureTokens[i].addChild(captureTokens.subList(i - 1, i + 2))
                captureTokens.removeAt(i + 1)
                captureTokens.removeAt(i - 1)
            } else
                ++i
        }
        i = 0
        while (i < captureTokens.size-1) {  // processing CONCAT
            val concatNode = Concat("+").createNode()
            concatNode.addChild(captureTokens.subList(i, i + 2))
            captureTokens.removeAt(i + 1)
            captureTokens.removeAt(i)
            captureTokens.add(i, concatNode)
        }
        return captureTokens[0]
    }

    private fun replaceNGtoCG(node: Node){  // replacing numbered groups with capture groups
        if (node.children.size == 0) {
            if (node is CaptureGroup) {
                node.set(copyNode(numberedGroups[node.n]!!))
            }
        }
        for (each in node.children) {
            replaceNGtoCG(each)
        }
    }

    private fun transformRepeats(node: Node) {    // replacing repeats with concat
        for (node in node.children) transformRepeats(node)
        for (n in node.children.indices) {
            if (node.children[n].getType() == TreeNodes.REPEAT) {
                if ((node.children[n].children.size == 1 && node.children[n].children[0].getType() == TreeNodes.EMPTY_LEAF) || (node.children[n] as Repeat).repeats == 0) {
                    node.children[n] = EmptyLeaf("from repeats").createNode()
                } else {
                    var newNode: Concat = Concat("from repeats")
                    var tmpNode: Node = newNode
                    for (i in 1 until (node.children[n] as Repeat).repeats) {
                        var childList = mutableListOf(copyNode(node.children[n].children.first()), copyNode(node.children[n].children.first()))
                        tmpNode.addChild(childList)
                        if (i < (node.children[n] as Repeat).repeats - 1)
                            tmpNode.children[0] = Concat().createNode()
                        tmpNode = tmpNode.children.first()
                    }
                    node.children[n] = newNode
                }
            }
        }
        /////////////
//        if (node.getType() == TreeNodes.REPEAT) {
//            if ((node.children.size == 1 && node.children[0].getType() == TreeNodes.EMPTY_LEAF) || (node as Repeat).repeats == 0) {
//                node.set(EmptyLeaf("from repeats"))
//            } else {
//                var newNode: Concat = Concat("from repeats")
//                var tmpNode: Node = newNode
//                for (i in 1 until node.repeats) {
//                    var childList = mutableListOf(copyNode(node.children.first()), copyNode(node.children.first()))
//                    tmpNode.addChild(childList)
//                    if (i < node.repeats - 1)
//                        tmpNode.children[0] = Concat().createNode()
//                    tmpNode = tmpNode.children.first()
//                }
//                node.set(newNode)
//            }
//        }
    }

    @Throws(NullPointerException::class)
    private fun callException(e: Int, placement: Int) {
        //TODO: return EXCEPTIONS
        //0-wrong regexp '\' on the position placement
        //1 - '|'
        //2 - *
        //3 - {} not digit or no }
        //4 - non existing numbered group
        //404 - something wrong
        throw  CustomExeptions("Hi!")
    }

//    fun makeCopy(node: Node): Node {
//        var newNode: Node = CaptureGroup("node")
//        for (each in node.children) {
//            if (each.children.isNotEmpty()) {
//                makeCopy(each)
//            } else {
//
//            }
//        }
//    }
}

private fun String.getSet(i: Int): String? {
    for (it in i+1 until this.length) {
        if (this[it] == ']' && this[it-1] != '\\') {
            return this.substring(i+1,  it)
        }
    }
    return null
}

private fun String.isDigit(): Boolean {
    this.forEach {
        if (!it.isDigit()) return false
    }
    return true
}

private fun String.countBr(): Int {
    var count = if (this[0] == '(') 1 else 0
    for (i in 1 until this.length) {
        if (this[i] == '(' && this[i-1] != '\\') ++count
    }
    return count
}
