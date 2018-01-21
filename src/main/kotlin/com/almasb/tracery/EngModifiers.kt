package com.almasb.tracery

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
private val modifiers = arrayListOf<Modifier>()

val ENG_MODIFIERS = load()

private fun load(): List<Modifier> {

    add("capitalize", { s, args ->
        if (s.isEmpty())
            return@add s

        val char0 = s[0]
        char0.toUpperCase() + s.drop(1)
    })


    add("s", { s, args ->
        s + "s"
    })

    add("a", { s, args ->
        if (s.isNotEmpty()) {
            if (s[0] in "uU") {
                if (s.length > 2) {
                    if (s[2] in "iI") {
                        return@add "a " + s
                    }
                }
            }

            if (s[0] in "aeiouAEIOU") {
                return@add "an " + s
            }
        }

        "a " + s
    })

    return modifiers
}

private fun add(name: String, func: (String, Array<String>) -> String) {

    modifiers.add(object : Modifier(name) {
        override fun apply(s: String, vararg args: String): String {
            return func.invoke(s, args.toList().toTypedArray())
        }
    })
}