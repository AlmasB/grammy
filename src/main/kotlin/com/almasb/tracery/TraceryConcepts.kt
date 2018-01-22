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

private val SYMBOL_OPERATOR = "#"
private val MODIFIER_OPERATOR = "."

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

        val sentence = firstSymbol.ruleset.joinToString(" ")

        return expand(sentence)
    }

    private fun expand(s: String): String {
        if (!s.hasSymbols())
            return s

        var result = s

        s.getSymbolKeys().forEach {

            // "it" is of form key.mod.mod where mods are optional

            // in case we have modifiers
            val symbolName = it.substringBefore(".")

            // TODO: symbol not found
            val newValue = symbols[symbolName]!!.selectRule().text

            var replacedValue = expand(newValue)

            if (it.hasModifiers()) {
                replacedValue = applyModifiers(replacedValue, it.split(".").drop(1))
            }

            result = result.replaceFirst("#$it#", replacedValue)
        }

        return result
    }

    /**
     * [input] is the text on which to apply modifiers.
     */
    private fun applyModifiers(input: String, modifierNames: List<String>): String {
        var result = input

        modifierNames.forEach { name ->

            // check if a modifier has params (function call)
            if (name.contains("(")) {
                // TODO: split the name and args, call apply appropriately
            } else {
                val modifier = ENG_MODIFIERS.find { it.name == name } ?: throw IllegalArgumentException("Modifier $name not found!")

                result = modifier.apply(result)
            }
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

private fun String.hasSymbols(): Boolean = this.contains(SYMBOL_OPERATOR)
private fun String.hasModifiers(): Boolean = this.contains(MODIFIER_OPERATOR)

private val symbolKeyPattern = Pattern.compile(Pattern.quote("#") + "(.*?)" + Pattern.quote("#"))

private fun String.getSymbolKeys(): List<String> {
    val m = symbolKeyPattern.matcher(this)

    val result = arrayListOf<String>()

    while (m.find()) {
        // given #key#, match == key
        val match = m.group(1)
        result.add(match)
    }

    return result
}