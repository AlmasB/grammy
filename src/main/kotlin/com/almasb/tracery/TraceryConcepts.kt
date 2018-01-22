package com.almasb.tracery

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.HashMap
import java.util.regex.Pattern

/*
 * The original specification of Tracery by Kate Compton can be found at https://github.com/galaxykate/tracery/tree/tracery2
 *
 * This implementation only loosely follows the original specification and given the same data may not produce the same output.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

/**
 * Currently just a text wrapper, but can be made more powerful in the future, e.g. with states.
 *
 * Examples: "some text", "#name#", "The color is #color#."
 */
class Rule(val text: String) {

    override fun toString(): String = text
}

/**
 * A symbol is a **key** (usually a short human-readable string) and a set of expansion rules.
 *
 * Each top-level key-value pair in the raw JSON object creates a **symbol**.
 * The symbol's **key** is set from the key, and the value determines the **ruleset**.
 *
 * Putting a **key** in hashtags, in a Tracery syntax object, will create a expansion node for that symbol within the text.
 */
class Symbol(val key: String, val ruleset: Set<Rule>) {

    /**
     * Selects a random single rule from the ruleset.
     */
    fun selectRule(): Rule {
        return ruleset.elementAt(Tracery.random.nextInt(ruleset.size))
    }
}

/**
 * A modifier is a function that takes a string (and optionally parameters) and returns a string.
 *
 * Modifiers are applied, in order, after a tag is fully expanded.
 *
 * To apply a modifier, add its name after a period, after the tag's main symbol:
 * #animal.capitalize#
 * #booktitle.capitalizeAll#
 * Hundreds of #animal.s#
 */
abstract class Modifier(val name: String) {

    abstract fun apply(s: String, vararg args: String): String
}

/**
 * TODO: action
 * An action that occurs when its node is expanded.
 *
 * Built-in actions are:
 * Generating some rules "[key:#rule#]" and pushing them to the "key" symbol's rule stack.
 * If that symbol does not exist, it creates it.
 */
class Action() {

}

/**
 * A Grammar is a dictionary of **symbols**.
 */
class Grammar() {

    private val symbols: HashMap<String, Symbol> = linkedMapOf()

    fun addSymbol(symbol: Symbol) {
        symbols[symbol.key] = symbol
    }

    fun flatten() = flatten("#origin#")

    fun flatten(startSymbolKey: String): String {

        // TODO: drop hashtags?
        val firstSymbol = symbols[startSymbolKey.drop(1).dropLast(1)] ?: throw IllegalArgumentException("Symbol key <$startSymbolKey> not found!")

        var sentence = firstSymbol.ruleset.joinToString(" ")

        return expand(sentence)
    }

    private fun expand(s: String): String {
        if (!s.contains('#'))
            return s

        var newS = s
        val symbolKeys = s.getSymbolKeys()

        symbolKeys.forEach {

            //println("parsing: $it")

            val symbolName = it.substringBefore(".")

            val newValue = symbols[symbolName]!!.selectRule().text

            var replacedValue = expand(newValue)

            // TODO: extract apply modifiers
            if (it.contains(".")) {
                val modifiers = it.split(".").drop(1)

                //println(modifiers)

                modifiers.forEach { modifierName ->

                    // TODO: if not found
                    // TODO: args
                    replacedValue = ENG_MODIFIERS.find { it.name == modifierName }!!.apply(replacedValue)
                }
            }

            newS = newS.replaceFirst("#$it#", replacedValue)
        }

        return newS
    }

    private fun String.getSymbolKeys(): List<String> {

        val m = Pattern.compile(Pattern.quote("#") + "(.*?)" + Pattern.quote("#")).matcher(this)

        val result = arrayListOf<String>()

        while (m.find()) {
            val match = m.group(1)
            result.add(match)
        }

        return result
    }

    /**
     * Rewrites this grammar with the one loaded from a JSON string.
     */
    fun fromJSON(json: String) {
        symbols.clear()

        val node = jacksonObjectMapper().readTree(json)

        node.fields().forEach {
            symbols.put(it.key, Symbol(it.key, it.value.asSequence().map { Rule(it.asText()) }.toSet()))
        }
    }

    /**
     * Converts this grammar to a JSON string.
     */
    fun toJSON(): String {
        val sb = StringBuilder()
        sb.append("{\n")

        symbols.forEach { key, symbol ->
            sb.append("  \"$key\": ${symbol.ruleset.joinToString(",", "[", "]", -1, "...", { "\"$it\"" })},\n")
        }

        // remove the last comma
        sb.deleteCharAt(sb.length-2)

        sb.append("}")

        return sb.toString()
    }
}