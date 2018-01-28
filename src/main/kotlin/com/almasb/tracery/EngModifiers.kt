package com.almasb.tracery

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
private val modifiers = arrayListOf<Modifier>()

val ENG_MODIFIERS = load()

private fun load(): List<Modifier> {

    // modifiers are applied after the tag is fully expanded
    // the expanded text, s, is guaranteed to be non-empty

    add("capitalize", { s, args ->
        s.first().toUpperCase() + s.drop(1)
    })

    add("capitalizeAll", { s, args ->
        s.toUpperCase()
    })

    add("s", { s, args ->
        when (s.last()) {
            in "shx" -> return@add s + "es"
            'y' -> {
                if (!s.dropLast(1).last().isVowel()) {
                    return@add s.dropLast(1) + "ies"
                }
            }
        }

        s + "s"
    })

    add("ed", { s, args ->
        return@add when (s.last()) {
            'e' -> s + "d"
            'y' -> {
                if (!s.dropLast(1).last().isVowel()) {
                    s.dropLast(1) + "ied"
                } else {
                    s + "d"
                }
            }
            else -> s + "ed"
        }
    })

    add("a", { s, args ->
//        if (s[0] in "uU" && s.length > 2 && s[2] in "iI") {
//            return@add "a " + s
//        }

        if (s[0].isVowel()) {
            return@add "an " + s
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

private fun Char.isVowel() = this in "aeiouAEIOU"