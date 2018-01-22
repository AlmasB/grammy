package com.almasb.tracery

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.HashMap
import java.util.regex.Pattern

/**
 * ## Library Concepts
### Grammar


### Symbol
A symbol is a **key** (usually a short human-readable string) and a set of expansion rules
 * the key
 * rulesetStack: the stack of expansion **rulesets** for this symbol.  This stack records the previous, inactive rulesets, and the current one.
 * optional connectivity data, such as average depth and average expansion length

Putting a **key** in hashtags, in a Tracery syntax object, will create a expansion node for that symbol within the text.

Each top-level key-value pair in the raw JSON object creates a **symbol**.  The symbol's *key* is set from the key, and the *value* determines the **ruleset**.

### Modifier
A function that takes a string (and optionally parameters) and returns a string.  A set of these is included in mods-eng-basic.js.  Modifiers are applied, in order, after a tag is fully expanded.

To apply a modifier, add its name after a period, after the tag's main symbol:
#animal.capitalize#
#booktitle.capitalizeAll#
Hundreds of #animal.s#

Modifiers can have parameters, too! (soon they will can have parameter that contain tags, which will be expanded when applying the modifier, but not yet)
#story.replace(he,she).replace(him,her).replace(his,hers)#

### Action
An action that occurs when its node is expanded.  Built-in actions are
 * Generating some rules "[key:#rule#]" and pushing them to the "key" symbol's rule stack.  If that symbol does not exist, it creates it.
 * Popping rules off of a rule stack, "[key:POP]"
 * Other functions

TODO: figure out syntax and implementation for generating *arrays* of rules, or other complex rulesets to push onto symbols' rulestacks

TODO: figure out syntax and storage for calling other functions, especially for async APIs.

### Ruleset
A ruleset is an object that defines a *getRule* function.  Calling this function may change the internal state of the ruleset, such as annotating which rules were most recently returned, or drawing and removing a rule from a shuffled list of available rules.

#### Basic ruleset
A basic ruleset is just an array of options.

They can be created by raw JSON by having an *array* or a *string* as the value, like this:
"someKey":["rule0", "rule1", "some#complicated#rule"]
If there is only one rule, it is acceptable short hand to leave off the array, but this only works with Strings.
"someKey":"just one rule"

These use the default distribution of the Grammar that owns them, which itself defaults to regular stateless pseudo-randomness.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

/**
 * A Grammar is a dictionary of symbols: a key-value object matching keys (the names of symbols) to expansion rules
 * optional metadata such as a title, edit data, and author
 * optional connectivity graphs describing how symbols call each other

 *clearState*: symbols and rulesets have state (the stack, and possible ruleset state recording recently called rules).
 * This function clears any state, returning the dictionary to its original state;

Grammars are usually created by feeding in a raw JSON grammar, which is then parsed into symbols and rules.
You can also build your own Grammar objects from scratch, without using this utility function, and can always edit the grammar after creating it.

 */
class Grammar() {

    private val symbols: HashMap<String, Symbol> = hashMapOf()

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

    fun fromJSON(json: String) {
        val typeRef = object : TypeReference<HashMap<String, Array<String>>>() {}

        val map: Map<String, Array<String>> = jacksonObjectMapper().readValue(json, typeRef)

        map.forEach { key, data ->
            symbols.put(key, Symbol(key, data.map { Rule(it) }.toSet()))
        }
    }

    fun toJSON(): String {
        // TODO:
        return jacksonObjectMapper().writeValueAsString(symbols)
    }
}

class Symbol(val key: String, val ruleset: Set<Rule>) {

    fun selectRule(): Rule {
        return ruleset.elementAt(Tracery.random.nextInt(ruleset.size))
    }
}

/**
 * Currently just a text wrapper, but can be made more powerful in the future, e.g. with states.
 */
class Rule(val text: String) {

    override fun toString(): String = text
}

abstract class Modifier(val name: String) {

    abstract fun apply(s: String, vararg args: String): String
}