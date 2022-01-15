package DataStructures

import kotlin.reflect.full.createInstance

const val meta: String = "|*{}[]()#"

open class Node(var value: String = "default") {
    open var symbol: String = value
    open var children: MutableList<Node> = mutableListOf()
    /**
     *  flags for DFA
     */
    var position = -1
    var nullable: Boolean = false
    var first: MutableList<Int> = mutableListOf()
    var last: MutableList<Int> = mutableListOf()

    open fun createNode(): Node? {
        return Node(symbol)
    }

    open fun getType(): TreeNodes? {
        return null
    }

    open fun addChild(tokens: MutableList<Node>){}

    fun set(node: Node) {
        this.value = node.value
        this.children = node.children
    }
}
enum class TreeNodes {
    LEAF,
    EMPTY_LEAF,
    OR,                 // 'r1|r2'      <=> '|'
    CONCAT,             // 'r1r2'       <=> '+'
    KLEENE_CLOSURE,     // 'r*'         <=> '*'
    REPEAT,             // 'r{n}'       <=> '{n}'
    SET,                // '[a1a2...]'  <=> '[...]'
    OPEN_PRIORITY,      // '('
    CLOSE_PRIORITY,     // ')'
    CAPTURE_GROUP,      // '(r)'
}

class Leaf(value: String = "default"): Node(value) {
    //override var symbol: String = value

    override fun createNode(): Leaf {
        return Leaf(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.LEAF
    }
}

class EmptyLeaf(value: String = "default"): Node(value) {
    //override var symbol: String = value

    override fun createNode(): EmptyLeaf {
        return EmptyLeaf(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.EMPTY_LEAF
    }
}

class Or(value: String = "default|"): Node(value) {
    override var symbol: String = "|"
    override fun createNode(): Or {
        return Or(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.OR
    }

    override fun addChild(captureTokens: MutableList<Node>) {
        //this.child = captureTokens[0]
        this.children.add(captureTokens[0])
        this.children.add(captureTokens[2])
    }
}

class Concat(value: String = "default+"): Node(value) {
    override var symbol: String = "+"
    //var leftChild:
    override fun createNode(): Concat {
        return Concat(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.CONCAT
    }

    override fun addChild(captureTokens: MutableList<Node>) {
        //this.child = captureTokens[0]
        this.children.add(captureTokens[0])
        this.children.add(captureTokens[1])
    }
}

class KleeneСlosure(value: String = "default*"): Node(value) {
    override var symbol: String = "*"
    override var children: MutableList<Node> = mutableListOf()
    //var child: Node? = null
    override fun createNode(): KleeneСlosure {
        return KleeneСlosure(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.KLEENE_CLOSURE
    }

    override fun addChild(captureTokens: MutableList<Node>) {
        //this.child = captureTokens[0]
        this.children.add(captureTokens[0])
    }
}

class Repeat(value: String = "default{}"): Node(value) {
    //override var symbol: String = "{n}"
    //var leftChild:
    var repeats: Int = value.substringAfter('{').substringBefore('}').toInt()
    override fun createNode(): Repeat {
        return Repeat(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.REPEAT
    }

    override fun addChild(captureTokens: MutableList<Node>) {
        this.children.add(captureTokens[0])
    }
}

class Set(value: String = "default[...]"): Node(value) {
    override var symbol: String = "[...]"

    override fun createNode(): Set? {
         var setNode = Set(value)
        setNode.addChild()
        return setNode
    }

    override fun getType(): TreeNodes {
        return TreeNodes.SET
    }

    fun addChild() {
        var i = 0
        while (i < value.length) {
            if (value[i] == '#')
                this.children.add(EmptyLeaf(value[i].toString()).createNode())
            else{
                if (value[i] == '\\') ++i
                this.children.add(Leaf(value[i].toString()).createNode())
            }
            ++i
        }
    }

}

class OpenPriority(value: String = "("): Node(value) {

    override fun createNode(): OpenPriority {
        return OpenPriority(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.OPEN_PRIORITY
    }
}

class ClosePriority(value: String = ")"): Node(value) {

    override fun createNode(): ClosePriority {
        return ClosePriority(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.CLOSE_PRIORITY
    }
}

class CaptureGroup(value: String = "default()"): Node(value) {
    override var symbol: String = "(r)"
    var tokens: MutableList<Node> = mutableListOf()
    //var child: Node? = null
    //override var children: MutableList<Node> = mutableListOf()
    var n: Int = 0
    constructor(captureTokens: MutableList<Node>) : this("node") {
        this.tokens = captureTokens
        //this.child = captureTokens[0]
        this.children.add(captureTokens[0])
    }
//    constructor() : this("default")

    override fun addChild(captureTokens: MutableList<Node>) {
        //this.child = captureTokens[0]
        this.children.add(captureTokens[0])
    }

    override fun createNode(): CaptureGroup {
        return CaptureGroup(value)
    }

    fun createNode(tokens: MutableList<Node>): CaptureGroup {
        this.tokens = tokens
        return CaptureGroup(value)
    }

    override fun getType(): TreeNodes {
        return TreeNodes.CAPTURE_GROUP
    }
}

fun copyNode(node: Node): Node {
    var newNode = node::class.createInstance().createNode()!!
    newNode.children = mutableListOf()
    newNode.symbol = node.symbol
    newNode.value = node.value
    for (child in node.children) {
        newNode.children.add(copyNode(child))
    }
    return newNode
}
