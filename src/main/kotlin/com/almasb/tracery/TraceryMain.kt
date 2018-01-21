package com.almasb.tracery

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.awt.SystemColor.text
import java.util.regex.Pattern


/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

class TraceryMain {

}

val node = jacksonObjectMapper().readTree(TraceryMain::class.java.getResourceAsStream("modifiers.json"))

fun main(args: Array<String>) {
    val START_SYMBOL = "sentence"

//    val grammar = Grammar(hashMapOf(
//            "animal".to(arrayOf("unicorn","raven","sparrow","scorpion","coyote","eagle","owl","lizard","zebra","duck","kitten")),
//            "name".to(arrayOf("Arjun","Yuuma","Darcy","Mia","Chiaki","Izzi","Azra","Lina"))
//    ))
//
//    val json = (grammar.toJSON())
//    grammar.fromJSON(json)




    // "sentence": ["The #color# #animal# of the #natureNoun# is called #name#"]
    val firstText = node.findValue(START_SYMBOL)

    var sentence = firstText.elements().asSequence().toList().map { it.asText() }.joinToString(" ")


    println(sentence)
    println(replace(sentence))


//    node.fields().forEach {
//        println(it.key)
//    }

//    node.elements().forEach {
//        println(it)
//    }

    //println(node["animal"])
}

private fun replace(s: String): String {
    if (!s.contains('#'))
        return s

    var newS = s
    val symbols = s.getSymbols()

    symbols.forEach {

        //println("parsing: $it")

        val symbolName = it.substringBefore(".")

        val newValue = node.findValue(symbolName).map { it.asText() }.random()

        var replacedValue = replace(newValue)

        if (it.contains(".")) {
            val modifiers = it.split(".").drop(1)

            //println(modifiers)

            modifiers.forEach {
                when (it) {
                    "capitalize" -> {
                        val char0 = replacedValue.take(1)

                        replacedValue = char0.toUpperCase() + replacedValue.drop(1)
                    }

                    "s" -> {
                        replacedValue = replacedValue + "s"
                    }

                    "a" -> {
                        replacedValue = "a " + replacedValue
                    }
                    else -> TODO(it)
                }
            }
        }

        newS = newS.replace("#$it#", replacedValue)
    }

    return newS
}

fun <T> List<T>.random(): T {
    return this[(this.size * Math.random()).toInt()]
}

fun String.getSymbols(): List<String> {

    val m = Pattern.compile(Pattern.quote("#") + "(.*?)" + Pattern.quote("#")).matcher(this)

    val result = arrayListOf<String>()

    while (m.find()) {
        val match = m.group(1)
        result.add(match)
    }

    return result
}