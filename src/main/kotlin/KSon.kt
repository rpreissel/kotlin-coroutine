class Kson {
    private val content: MutableMap<String, String> = mutableMapOf()

    operator fun String.plusAssign(other: Any) {
        content[this] = other.toString()
    }

    operator fun String.plusAssign(other: String) {
        content[this] = """"$other""""
    }

    operator fun String.plusAssign(body: Kson.() -> Unit) {
        content[this] = Kson().apply(body).toString()
    }

    override fun toString() =
        content.toList().map { """ "${it.first}" : ${it.second} """ }.joinToString(prefix = "{", postfix = "}")

}

fun kson(body: Kson.() -> Unit) =
    Kson().apply(body).toString()


fun main(args: Array<String>) {
    val kson = kson {
        "name" += "Rene"
        "adresse" += {
            "stadt" += "Hamburg"
            "plz" += 22391
        }
    }

    println(kson) //{ "name" : "Rene" ,  "adresse" : { "stadt" : "Hamburg" ,  "plz" : 22391 } }
}