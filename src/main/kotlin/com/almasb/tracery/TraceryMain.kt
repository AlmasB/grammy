package com.almasb.tracery

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

class TraceryMain {

}

//val node = jacksonObjectMapper().readTree(TraceryMain::class.java.getResourceAsStream("modifiers.json"))

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
    //val firstText = node.findValue(START_SYMBOL)

    //var sentence = firstText.elements().asSequence().toList().map { it.asText() }.joinToString(" ")


    //println(sentence)
    //println(replace(sentence))
}

