package com.almasb.grammy.editor

import com.almasb.grammy.core.Grammy

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class RandomStoryCreator {

    // TODO: words for fwd ref, signpost and things like "however, although, despite"
    fun createStoryAsJSON(): String {
        val grammar = com.almasb.grammy.core.Grammy.createGrammar()

        val sentenceFormats = listOf(
                "{noun#.{4}#.capitalize} {verb.s} {noun.a}.",
                "{noun.capitalize} {verb.ed}!",
                "{noun.capitalize} {verb.ed}.",
                "{hero} {verb.s} and {verb.s}.",
                "{adj.capitalize.optional(55)} {hero} {verb.ed} {noun.a}, then {verb.ed}.",
                "{adj.capitalize} {noun} {verb.ed} {noun.s}.",
                "{hero} {verb.s} {noun.a}.",
                "{hero} {verb.ed}!",
                "{friend} {verb.ed} too.",
                "{hero} {verb} {adj} {noun.s} and they are {adj}.",
                "{adj.capitalize.optional(95)} {friend} always ready to {verb} {hero}."
        )

        val rareSentenceFormats = arrayListOf(
                "It was truly {adj} for {hero}'s {friend}",
                "Interestingly, {adj.optional(33)} {noun.s} {verb.ed}.",
                "However, {noun.s} {verb.ed} their {noun.s}.",
                "Nevertheless, {hero} {verb.ed}.",
                "Actually, they {verb} {noun.s}, so they never {verb.ed} before.",
                "Of course, {hero} will {verb} their {adj} {noun.s}.",
                "Unexpectedly, our {friend} suddenly {verb} {noun.s} together with {hero}.",
                "But wait! {adj.capitalize.optional(95)} {hero} can also {verb} {noun.a}.",
                "{adj.capitalize.optional(95)} {friend} just {verb} {noun.s}, which is why {hero} {verb} {adj.optional(35)} {noun.s}."
        )

        val sentences = arrayListOf<String>()

        repeat(20) {
            sentences += sentenceFormats.random()

            if (Math.random() < 0.1 && rareSentenceFormats.isNotEmpty()) {
                val s = rareSentenceFormats.random()
                sentences += s
                rareSentenceFormats -= s
            }

            if (Math.random() < 0.8) {
                sentences += sentenceFormats.random()
            }

            if (Math.random() < 0.08) {
                sentences += "\\n\\n"
            }
        }

        grammar.addSymbol("friendName", listOf("dragon", "camel", "rat", "cat", "dog", "tiger"))
        grammar.addSymbol("hero", listOf("Hero"))
        grammar.addSymbol("origin", listOf("[friend:{friendName}]") + sentences)

        return grammar.toJSON()
    }

    fun createSentence(): String {
        return "{noun.capitalize} {verb.s} {noun.a}."
    }
}