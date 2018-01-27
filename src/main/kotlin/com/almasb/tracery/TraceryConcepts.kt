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

    private val distributions = hashMapOf<IntRange, Rule>()
    private val withoutDistr: Set<Rule>

    init {
        val withDistributions = ruleset.filter { it.text.endsWith(")") }
        withoutDistr = ruleset.minus(withDistributions)

        var bound = 0

        withDistributions.forEach {
            val dist = it.text.substringAfter("(").substringBefore(")").toInt()

            val range = bound until bound+dist

            distributions[range] = Rule(it.text.removeSuffix("($dist)"))

            bound += dist
        }

        if (bound > 100) {
            throw IllegalStateException("Distributions for $key don't add up to 100%")
        }
    }

    /**
     * Selects a random single rule from the ruleset.
     */
    fun selectRule(regex: String): Rule {
        // TODO: when using regex also take into account distributions?

        if (regex.isNotEmpty()) {
            val valid = distributions.values.plus(withoutDistr).filter { it.text.matches(regex.toRegex()) }

            if (valid.isEmpty()) {
                throw IllegalStateException("No matching rule found!")
            }

            return valid[Tracery.random.nextInt(valid.size)]
        }


        if (distributions.isNotEmpty()) {
            val randomValue = Tracery.random.nextInt(100)

            for ((distr, rule) in distributions) {
                if (randomValue in distr) {
                    return rule
                }
            }
        }

        return withoutDistr.elementAt(Tracery.random.nextInt(withoutDistr.size))
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
    private val runtimeSymbols: HashMap<String, Symbol> = linkedMapOf()

    fun addSymbol(symbol: Symbol) {
        symbols[symbol.key] = symbol
    }

    fun flatten() = flatten("origin")

    fun flatten(startSymbolKey: String): String {

        val firstSymbol = symbols[startSymbolKey] ?: throw IllegalArgumentException("Symbol key \"$startSymbolKey\" not found!")

        val sentence = firstSymbol.ruleset.joinToString(" ")

        return expand(sentence)
    }

    //fun parseText(text: String): String {
//    val oldText = StringBuilder(text)
//    val newText = StringBuilder()
//
//    var firstTag = oldText.indexOf("{")
//    FullBreak@ while (firstTag >= 0) {
//        newText.append(oldText.substring(0, firstTag))
//        oldText.delete(0, firstTag)
//        var depth = 1
//        var position = 0
//        while (depth > 0) {
//            position++
//            if (position > oldText.length - 1) {
//                break@FullBreak
//            }
//            if (oldText[position] == '{' && oldText[position - 1] != '\\') {
//                depth++
//            }
//            if (oldText[position] == '}' && oldText[position - 1] != '\\') {
//                depth--
//            }
//        }
//        position++
//        newText.append(parseTag(oldText.substring(0, position)).render())
//        oldText.delete(0, position)
//        firstTag = oldText.indexOf("{")
//    }
//    newText.append(oldText)
//    return newText.toString()
//}

    private fun expand(s: String): String {
        if (!s.contains("{"))
            return s

        var result = s

        // TODO: standalone action tags?

        while (result.contains("{")) {

            var symbolTagIndex = result.indexOf('{')
            var actionTagIndex = 0

            var hitRegex = false

            for (i in symbolTagIndex + 1 until result.length) {

                if (result[i] == '{') {

                    // only if not within regex
                    if (hitRegex)
                        continue

                    symbolTagIndex = i

                } else if (result[i] == '[') {

                    if (hitRegex)
                        continue

                    actionTagIndex = i

                } else if (result[i] == ']') {

                    if (hitRegex)
                        continue

                    val action = result.substring(actionTagIndex + 1, i)

                    // do action
                    val tokens = action.split(":")

                    // assume inside already expanded?

                    runtimeSymbols[tokens[0]] = Symbol(tokens[0], setOf(Rule(tokens[1])))

                    // and clear action from result text
                    result = result.replaceRange(actionTagIndex, i+1, "")
                    break

                } else if (result[i] == '#') {

                    hitRegex = !hitRegex

                } else if (result[i] == '}') {

                    // only if not within regex
                    if (hitRegex)
                        continue



                    val key = result.substring(symbolTagIndex + 1, i)

                    // "it" is of form key#regex#.mod.mod where mods are optional

                    val symbolName = if (key.contains("#")) {
                        key.substringBefore("#")
                    } else {
                        // in case we have modifiers
                        key.substringBefore(".")
                    }

                    // TODO: generalize


                    val newValue = if (symbolName == "num") {
                        Tracery.random.nextInt(Int.MAX_VALUE).toString()
                    } else {
                        val regex = if (key.contains("#")) key.substringAfter("#").substringBefore("#") else ""

                        getSymbol(symbolName).selectRule(regex).text
                    }

                    var replacedValue = expand(newValue)

                    if (key.hasModifiers()) {
                        // clean from regex then apply mods
                        replacedValue = applyModifiers(replacedValue, key.substringAfterLast("#").split(".").drop(1))
                    }

                    result = result.replaceRange(symbolTagIndex, i+1, replacedValue)
                    break
                }
            }
        }

        return result
    }

    private fun getSymbol(name: String): Symbol {
        return symbols[name] ?: runtimeSymbols[name] ?: throw IllegalArgumentException("Symbol key \"$name\" not found!")
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