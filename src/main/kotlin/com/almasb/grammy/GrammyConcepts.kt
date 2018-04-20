package com.almasb.grammy

import com.almasb.grammy.Grammy.random
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.HashMap

/*
 * The original specification of Grammy by Kate Compton can be found at https://github.com/galaxykate/tracery/tree/tracery2
 *
 * This implementation introduces new concepts and more functionality via new syntax.
 * The syntax used here is incompatible with the original.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

// These are reserved Grammy characters
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
            throw error("Rule cannot be empty")
    }

    override fun toString(): String = text
}

/**
 * A symbol is a **key** (a non-empty string) and a non-empty set of expansion rules.
 *
 * Each top-level key-value pair in the raw JSON object creates a **symbol**.
 * The pair key becomes the symbol's **key**, and the pair value determines the **ruleset**.
 *
 * Placing a **key** between '{' and '}', in a Grammy syntax object, will create a expansion node for that symbol within the text.
 * Example: "The color is {color}."
 *
 * An action can be used to generate runtime symbols.
 * An action is placed between '[' and ']' and before the symbol key.
 * Example: {[hero:Stephen,John]key} or more complex {[hero:{name}]key}.
 * This syntax will generate a symbol with key "hero" and ruleset "Stephen", "John".
 * If the symbol does not exist, the action creates it.
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
            throw error("Symbol key cannot be empty")

        if (ruleset.isEmpty())
            throw error("Ruleset cannot be empty")

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
            throw error("Rule distributions for $key are greater than 100%")
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
                throw error("No matching rule found")
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
        val story = getSymbol(startSymbolKey).ruleset.joinToString(" ")

        return expand(story)
    }

    /**
     * Fully expand given string, i.e. the returned value will not have symbols.
     */
    private fun expand(s: String): String {
        if (!s.hasSymbols())
            return s

        var result = s

        // TODO: or actions?
        while (result.hasSymbols()) {
            result = expandSymbolOrAction(result)
        }

        return result
    }

    private fun expandSymbolOrAction(s: String): String {
        var symbolTagIndex = s.indexOf(SYMBOL_START)
        var actionTagIndex = 0

        var insideRegex = false

        for (index in symbolTagIndex + 1 until s.length) {
            if (s[index] == REGEX_DELIMITER) {
                insideRegex = !insideRegex
                continue
            }

            if (insideRegex)
                continue

            when (s[index]) {
                SYMBOL_START -> { symbolTagIndex = index }

                ACTION_START -> { actionTagIndex = index }

                SYMBOL_END -> {
                    val key = s.substring(symbolTagIndex + 1, index)

                    val expandedText = expandSymbol(key)

                    var extraChars = 1

                    // applying modifiers can result in text being empty
                    // so need to compensate for extra space either in front or before
                    if (expandedText.isEmpty()) {
                        // use previous space char
                        if (symbolTagIndex > 0) {
                            symbolTagIndex--

                            // else use next space char
                        } else if (index + 1 < s.length) {
                            extraChars = 2
                        }
                    }

                    return s.replaceRange(symbolTagIndex, index + extraChars, expandedText)
                }

                ACTION_END -> {
                    val action = s.substring(actionTagIndex + 1, index)

                    expandAction(action)

                    // and clear action from result text
                    return s.replaceRange(actionTagIndex, index+1, "")
                }
            }
        }

        throw error("No symbol or action found")
    }

    /**
     * Fully expands 1 symbol occurrence.
     *
     * [key] has a maximal form: key#regex#.mod.mod.etc
     */
    private fun expandSymbol(key: String): String {
        // if we have regex or modifier, then clean it
        // if we don't have either, then the original string is returned
        val symbolName = key.substringBefore( if (key.hasRegex()) REGEX_DELIMITER else MODIFIER_OPERATOR )

        // TODO: generalize by creating special symbols
        val newValue = if (symbolName == "num") {
            random.nextInt(Int.MAX_VALUE).toString()
        } else {
            val regex = if (key.hasRegex()) key.substringBetween(REGEX_DELIMITER) else ""

            getSymbol(symbolName).selectRule(regex).text
        }

        // fully expand new value too in case of nested symbols
        var expandedText = expand(newValue)

        if (key.hasModifiers()) {
            // clean from regex then apply mods
            expandedText = applyModifiers(expandedText, key.substringAfterLast(REGEX_DELIMITER).split(MODIFIER_OPERATOR).drop(1))
        }

        return expandedText
    }

    /**
     * [action] has a maximal form key:rule1,rule2,rule3,etc
     */
    private fun expandAction(action: String) {
        if (action.isNotEmpty()) {
            val tokens = action.split(ACTION_OPERATOR)

            val key = tokens[0]
            val ruleset = tokens[1].split(MULTIPLE_ACTION_DELIMITER).map { Rule(it) }.toSet()

            runtimeSymbols[key] = Symbol(key, ruleset)
        }
    }

    private fun getSymbol(name: String): Symbol {
        return symbols[name] ?: runtimeSymbols[name] ?: throw error("Symbol key \"$name\" not found!")
    }

    /**
     * [input] is the text on which to apply modifiers.
     */
    private fun applyModifiers(input: String, modifierNames: List<String>): String {
        var result = input

        modifierNames.forEach { name ->

            // check if a modifier has params (function call)
            if (name.contains('(')) {

                val modName = name.substringBefore('(')

                val params = name.substringAfter('(').substringBefore(')').split(",")

                val modifier = ENG_MODIFIERS.find { it.name == modName } ?: throw IllegalArgumentException("Modifier $modName not found!")

                result = modifier.apply(result, *params.toTypedArray())

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

        val rootObject = ObjectMapper().readTree(json)

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

private fun error(message: String) = TracerySyntaxException(message)

private fun String.hasSymbols(): Boolean = this.contains(SYMBOL_START)
private fun String.hasRegex(): Boolean = this.contains(REGEX_DELIMITER)
private fun String.hasModifiers(): Boolean = this.contains(MODIFIER_OPERATOR)

private fun String.substringBetween(delimiter: Char): String = this.substringAfter(delimiter).substringBefore(delimiter)

