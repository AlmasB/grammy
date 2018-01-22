package com.almasb.tracery

import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class TraceryTest {

    // TODO: extract common code from tests

    @Test
    fun `Basic syntax 1`() {

        val json = readJSON("simple1.json")

        val grammar = Tracery.createGrammar(json)
        val expandedText = grammar.flatten("#animal#")

        assertThat(expandedText, `is`("unicorn raven"))
    }

    @Test
    fun `Basic syntax 2`() {

        val json = readJSON("simple2.json")

        val grammar = Tracery.createGrammar(json)
        val expandedText = grammar.flatten("#randomAnimal#")

        assertThat(expandedText, either(`is`("unicorn")).or(`is`("raven")))
    }

    @Test
    fun `Basic syntax 3`() {

        Tracery.setRandom(Random(0))

        val json = readJSON("simple3.json")

        val grammar = Tracery.createGrammar(json)
        var expandedText = grammar.flatten("#sentence#")

        assertThat(expandedText, `is`("The purple owl of the river is called Chiaki"))

        Tracery.setRandom(Random(1))
        expandedText = grammar.flatten("#sentence#")

        assertThat(expandedText, `is`("The purple owl of the mountain is called Mia"))
    }

    @Test
    fun `The same symbol expands to random text`() {

        Tracery.setRandom(Random(0))

        val json = readJSON("simple4.json")

        val grammar = Tracery.createGrammar(json)
        val expandedText = grammar.flatten("#sentence#")

        val tokens = expandedText.split(",")

        assertThat(tokens[0], `is`(not(tokens[1])))
    }

    @Test
    fun `Modifiers`() {

        Tracery.setRandom(Random(15))

        val json = readJSON("modifiers.json")

        val grammar = Tracery.createGrammar(json)
        val expandedText = grammar.flatten("#sentence#")

        assertThat(expandedText, `is`("Purple unicorns are always wistful. An owl is often courteous, unless it is an orange one."))
    }

    @Test
    fun `Recursive`() {

        Tracery.setRandom(Random(15))

        val json = readJSON("recursive.json")

        val grammar = Tracery.createGrammar(json)
        val expandedText = grammar.flatten("#sentence#")

        assertThat(expandedText, `is`("The purple unicorn of the mountain is called Darcy"))
    }

    @Test
    fun `Write to JSON`() {
        val json = readJSON("modifiers.json")

        val grammar = Tracery.createGrammar(json)
        val generated = grammar.toJSON()

        assertThat(generated.replace("\n", ""), `is`(json))
    }

    private fun readJSON(fileName: String): String {
        return Files.readAllLines(Paths.get(javaClass.getResource(fileName).toURI())).joinToString("")
    }
}