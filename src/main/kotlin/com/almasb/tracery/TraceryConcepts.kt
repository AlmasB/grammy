package com.almasb.tracery

import com.almasb.tracery.Tracery.random
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.HashMap

/*
 * The original specification of Tracery by Kate Compton can be found at https://github.com/galaxykate/tracery/tree/tracery2
 *
 * This implementation introduces new concepts and more functionality via new syntax.
 * The syntax used here is incompatible with the original.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

// These are reserved Tracery characters
private val SYMBOL_START = '{'
private val SYMBOL_END = '}'
private val ACTION_START = '['
private val ACTION_END = ']'
private val ACTION_OPERATOR = ':'
private val MULTIPLE_ACTION_DELIMITER = ','
private val DISTRIBUTION_START = '('
private val DISTRIBUTION_END = ')'
private val REGEX_DELIMITER = '#'
private val MODIFIER_OPERATOR = '.'

/**
 * A rule is a non-empty string.
 *
 * Examples: "some text", "{name}", "The color is {color}."
 */
class Rule(val text: String) {

    init {
        if (text.isEmpty())
            fail("Rule cannot be empty")
    }

    override fun toString(): String = text
}

/**
 * A symbol is a **key** (a non-empty string) and a non-empty set of expansion rules.
 *
 * Each top-level key-value pair in the raw JSON object creates a **symbol**.
 * The pair key becomes the symbol's **key**, and the pair value determines the **ruleset**.
 *
 * Placing a **key** between '{' and '}', in a Tracery syntax object, will create a expansion node for that symbol within the text.
 * Example: "The color is {color}."
 *
 * Each rule can have a distribution percentage value in range [0..100] associated with it.
 * The sum of all distribution values per symbol cannot exceed 100.
 * Examples: "dog(30)", "cat(15)", "mouse", "pig".
 * Dog and cat will have respectively 30% and 15% chance of being selected, whereas mouse and pig
 * will share the remaining 55% and if the 55% is selected, randomly one of them will be chosen.
 */
class Symbol(val key: String, val ruleset: Set<Rule>) {

    private val distributions = hashMapOf<IntRange, Rule>()
    private val withoutDistr: Set<Rule>

    init {
        if (key.isEmpty())
            fail("Symbol key cannot be empty")

        if (ruleset.isEmpty())
            fail("Ruleset cannot be empty")

        val withDistributions = ruleset.filter { it.text.endsWith(")") }
        withoutDistr = ruleset.minus(withDistributions)

        var bound = 0

        withDistributions.forEach {
            val dist = it.text.substringAfter(DISTRIBUTION_START).substringBefore(DISTRIBUTION_END).toInt()

            val range = bound until bound+dist

            distributions[range] = Rule(it.text.removeSuffix("($dist)"))

            bound += dist
        }

        if (bound > 100) {
            fail("Rule distributions for $key are greater than 100%")
        }
    }

    /**
     * Selects a random single rule from the ruleset.
     * If a rule has a distribution associated with it, the distribution will be honored.
     */
    fun selectRule(regex: String): Rule {
        // TODO: when using regex also take into account distributions?

        if (regex.isNotEmpty()) {
            val valid = distributions.values.plus(withoutDistr).filter { it.text.matches(regex.toRegex()) }

            if (valid.isEmpty()) {
                throw IllegalStateException("No matching rule found!")
            }

            return valid[random.nextInt(valid.size)]
        }

        if (distributions.isNotEmpty()) {
            val randomValue = random.nextInt(100)

            for ((distr, rule) in distributions) {
                if (randomValue in distr) {
                    return rule
                }
            }
        }

        return withoutDistr.elementAt(random.nextInt(withoutDistr.size))
    }
}

/**
 * A modifier is a function that takes a string (and optionally parameters) and returns a string.
 * A modifier can only be applied to a symbol key.
 *
 * Modifiers are applied, in order, after a tag is fully expanded.
 *
 * To apply a modifier, add its name after a period, after the tag's main symbol:
 * {animal.capitalize}
 * {booktitle.capitalizeAll}
 * Hundreds of {animal.s}
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
class Grammar {

    private val symbols: HashMap<String, Symbol> = linkedMapOf()
    private val runtimeSymbols: HashMap<String, Symbol> = linkedMapOf()

    fun addSymbol(symbolKey: String, ruleset: Set<String>) {
        symbols[symbolKey] = Symbol(symbolKey, ruleset.map { Rule(it) }.toSet())
    }

    fun flatten() = flatten("origin")

    fun flatten(startSymbolKey: String): String {
        val firstSymbol = symbols[startSymbolKey] ?: throw parseError("Symbol key not found: $startSymbolKey")

        val story = firstSymbol.ruleset.joinToString(" ")

        return expand(story)
    }

    private fun expand(s: String): String {
        if (!s.hasSymbols())
            return s

        var result = s

        while (result.hasSymbols()) {

            var symbolTagIndex = result.indexOf(SYMBOL_START)
            var actionTagIndex = 0

            var insideRegex = false

            charLoop@
            for (index in symbolTagIndex + 1 until result.length) {

                val currentChar = result[index]

                if (currentChar == REGEX_DELIMITER) {
                    insideRegex = !insideRegex
                    continue
                }

                if (insideRegex)
                    continue

                when (currentChar) {
                    SYMBOL_START -> {
                        symbolTagIndex = index
                    }

                    ACTION_START -> {
                        actionTagIndex = index
                    }

                    // TODO: extract into functions
                    SYMBOL_END -> {
                        val key = result.substring(symbolTagIndex + 1, index)

                        // key has maximal form: key#regex#.mod.mod
                        // so if we have regex or modifier, if we don't have either, then the original string is returned
                        val symbolName = key.substringBefore( if (key.hasRegex()) REGEX_DELIMITER else MODIFIER_OPERATOR )





                        // TODO: generalize
                        val newValue = if (symbolName == "num") {
                            random.nextInt(Int.MAX_VALUE).toString()
                        } else {
                            val regex = if (key.hasRegex()) key.substringBetween(REGEX_DELIMITER) else ""

                            getSymbol(symbolName).selectRule(regex).text
                        }

                        var expandedText = expand(newValue)

                        if (key.hasModifiers()) {
                            // clean from regex then apply mods
                            expandedText = applyModifiers(expandedText, key.substringAfterLast(REGEX_DELIMITER).split(MODIFIER_OPERATOR).drop(1))
                        }

                        // update result
                        result = result.replaceRange(symbolTagIndex, index+1, expandedText)
                        break@charLoop
                    }

                    ACTION_END -> {
                        val action = result.substring(actionTagIndex + 1, index)

                        if (action.isNotEmpty()) {
                            val tokens = action.split(ACTION_OPERATOR)

                            // action is of form key:rule1,rule2,rule3
                            // so tokens[0] is key
                            // and tokens[1] is rule1,rule2,rule3

                            runtimeSymbols[tokens[0]] = Symbol(tokens[0], tokens[1].split(MULTIPLE_ACTION_DELIMITER).map { Rule(it) }.toSet())
                        }

                        // and clear action from result text
                        result = result.replaceRange(actionTagIndex, index+1, "")
                        break@charLoop
                    }
                }
            }
        }

        return result
    }

    private fun getSymbol(name: String): Symbol {
        return symbols[name] ?: runtimeSymbols[name] ?: throw parseError("Symbol key \"$name\" not found!")
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
        runtimeSymbols.clear()

        val rootObject = jacksonObjectMapper().readTree(json)

        rootObject.fields().forEach {
            addSymbol(it.key, it.value.asSequence().map { it.asText() }.toSet())
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

class TracerySyntaxException(message: String) : RuntimeException(message)
class TraceryParseException(message: String) : RuntimeException(message)

private fun fail(message: String) {
    throw TracerySyntaxException(message)
}

private fun parseError(message: String) = TraceryParseException(message)

private fun String.hasSymbols(): Boolean = this.contains(SYMBOL_START)
private fun String.hasRegex(): Boolean = this.contains(REGEX_DELIMITER)
private fun String.hasModifiers(): Boolean = this.contains(MODIFIER_OPERATOR)

private fun String.substringBetween(delimiter: Char): String = this.substringAfter(delimiter).substringBefore(delimiter)

