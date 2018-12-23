@file:UseExperimental(ExperimentalContracts::class)

import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KClass


class Person(val name: String)
class TableAlias(val kClass: KClass<*>, val alias: String?) {
    val table: String get() = kClass.simpleName ?: throw IllegalStateException("class without name: ${kClass}")
    override fun toString(): String = alias?.let { "$table $alias" } ?: table
    operator fun invoke(vararg names: String): String = names
        .flatMap {
            it.split("\\s*,\\s*".toRegex())
        }
        .map { name ->
            alias?.let { "$alias.$name as ${alias}_$name" } ?: name
        }.joinToString(", ")
}

class QueryContext {
    private val query: StringBuilder = StringBuilder()
    private val variables = mutableListOf<Pair<Any?, KClass<*>>>()
    inline fun <reified T> table(alias: String? = null) = TableAlias(T::class, alias)
    inline fun <reified T : Any> variable(value: T?): String = variable(value, T::class)
    inline fun <reified T : Any> multiVariable(values: Iterable<T>, separator: CharSequence = ", "): String =
        multiVariable(values, T::class, separator)

    @PublishedApi
    internal fun <T : Any> variable(value: T?, kClass: KClass<T>): String {
        variables.add(value to kClass)
        return "?:${variables.size - 1}:?"
    }

    @PublishedApi
    internal fun <T : Any> multiVariable(
        values: Iterable<T>,
        kClass: KClass<T>,
        separator: CharSequence
    ): String {
        return values.map { variable(it, kClass) }.joinToString(separator)
    }

    fun add(part: String) {
        query.appendln(part)
    }

    fun prepareStatement(): Pair<String, List<Any?>> {
        val paras = mutableListOf<Any?>()
        val replacedQuery = PARA_REGEXP.replace(query) { mr ->
            paras += variables[mr.groups[1]?.value?.toInt() ?: throw IllegalStateException()]
            "?"
        }

        return replacedQuery to paras
    }

    companion object {
        private val PARA_REGEXP = Regex("\\?:(\\d+):\\?")
    }
}


fun main(args: Array<String>) {
    queryPerson("rene", listOf(1, 2, 3), true)
}

private fun queryPerson(name: String?, ids: List<Int>, vip: Boolean) {
    query {
        val p1 = table<Person>("p1")
        val p2 = table<Person>("p2")
        add(
            """
            Select
                ${p1("name,vorname")},
                ${p2("name", "vorname")}
            From $p1 left join $p2 on p1.name = p2.name
            Where
                1 = 1
            """
        )

        addIfNotNull(name) {
            """
                AND name=${variable(it)}
            """
        }
        add {
            ifNotNull(name) {
                """
                    AND name=${variable(it)}
                """
            }

            ifTrue(vip) {
                """
                   AND Vip = 1
                """
            }

            ifFalse(vip) {
                """
                   AND Vip = 0
                """
            }

            ifTrue(ids.isNotEmpty()) {
                """
                   AND id in (${multiVariable(ids)})
                """
            }

            otherwise {
                """
                   AND 0 = 0
                """
            }
        }
    }
}


fun <T> QueryContext.addIfNotNull(value: T?, block: QueryContext.(T) -> String) {
    add {
        ifNotNull(value, block)
    }
}


inline fun QueryContext.add(block: AddContext.() -> Unit) {
    val addContext = AddContext()
    addContext.block()
    addContext.evaluate(this)
}

class AddContext() {
    internal val caseBlocks = mutableListOf<QueryContext.() -> String>()
    internal val otherwiseBlocks = mutableListOf<QueryContext.() -> String>()
    fun evaluate(queryContext: QueryContext) {
        caseBlocks.forEach {
            queryContext.run {
                add(it())
            }
        }

        if (caseBlocks.isEmpty()) {
            otherwiseBlocks.forEach {
                queryContext.run {
                    add(it())
                }
            }
        }
    }
}


fun AddContext.ifTrue(value: Boolean, block: QueryContext.() -> String) {
    if (value) {
        caseBlocks += block
    }
}

fun AddContext.ifFalse(value: Boolean, block: QueryContext.() -> String) {
    if (!value) {
        caseBlocks += block
    }
}

fun <T> AddContext.ifNotNull(value: T?, block: QueryContext.(T) -> String) {
    ifTrue(value != null) {
        block(value!!)
    }
}

fun AddContext.otherwise(block: QueryContext.() -> String) {
    otherwiseBlocks += block
}


fun query(block: QueryContext.() -> Unit) {
    val queryContext = QueryContext()
    queryContext.block()
    val (query, paras) = queryContext.prepareStatement()

    query.lineSequence().filter { it.trim().isNotEmpty() }.forEach {
        println(it)
    }

    println(paras.joinToString(prefix = "[", postfix = "]"))
}
