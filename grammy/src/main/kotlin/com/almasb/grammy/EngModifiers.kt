package com.almasb.grammy

import java.util.*

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

    add("capitalize") { s, _ ->
        s.first().uppercase() + s.drop(1)
    }

    add("capitalizeAll") { s, _ ->
        s.uppercase()
    }

    add("s") { s, _ ->
        when (s.last()) {
            in "shx" -> return@add s + "es"
            'y' -> {
                if (!s.dropLast(1).last().isVowel()) {
                    return@add s.dropLast(1) + "ies"
                }
            }
        }

        s + "s"
    }

    add("ed") { s, _ ->
        return@add when (s.last()) {
            'e' -> s + "d"
            'y' -> {
                if (!s.dropLast(1).last().isVowel()) {
                    s.dropLast(1) + "ied"
                } else {
                    s + "ed"
                }
            }
            else -> s + "ed"
        }
    }

    add("a") { s, _ ->
        if (s[0].isVowel()) {
            return@add "an $s"
        }

        "a $s"
    }

    add("optional") { random, s, args ->
        val chance = if (args.isNotEmpty()) args[0].toInt() else 50

        if (random.nextInt(100) < chance)
            s
        else
            ""
    }

    return modifiers
}

private fun add(name: String, func: (String, Array<String>) -> String) {

    modifiers.add(object : Modifier(name) {
        override fun apply(random: Random, s: String, vararg args: String): String {
            return func.invoke(s, args.toList().toTypedArray())
        }
    })
}

private fun add(name: String, func: (Random, String, Array<String>) -> String) {

    modifiers.add(object : Modifier(name) {
        override fun apply(random: Random, s: String, vararg args: String): String {
            return func.invoke(random, s, args.toList().toTypedArray())
        }
    })
}

private fun Char.isVowel() = this in "aeiouAEIOU"