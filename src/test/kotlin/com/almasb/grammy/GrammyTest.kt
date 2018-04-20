package com.almasb.grammy

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class GrammyTest {

    // TODO: extract common code from tests
    // TODO: use @BeforeEach to set up grammy random

    @Test
    fun `Basic syntax 1`() {
        val json = readJSON("simple1.json")

        val grammar = Grammy.createGrammar(json)
        val expandedText = grammar.flatten("animal")

        assertThat(expandedText, `is`("unicorn raven"))
    }

    @Test
    fun `Basic syntax 2`() {
        val json = readJSON("simple2.json")

        val grammar = Grammy.createGrammar(json)
        val expandedText = grammar.flatten("randomAnimal")

        assertThat(expandedText, either(`is`("unicorn")).or(`is`("raven")))
    }

    @Test
    fun `Basic syntax 3`() {
        Grammy.setRandom(Random(0))

        val json = readJSON("simple3.json")

        val grammar = Grammy.createGrammar(json)
        var expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("The purple owl of the river is called Chiaki"))

        Grammy.setRandom(Random(1))
        expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("The purple owl of the mountain is called Mia"))
    }

    @Test
    fun `The same symbol expands to random text`() {
        Grammy.setRandom(Random(0))

        val json = readJSON("simple4.json")

        val grammar = Grammy.createGrammar(json)
        val expandedText = grammar.flatten("sentence")

        val tokens = expandedText.split(",")

        assertThat(tokens[0], `is`(not(tokens[1])))
    }

    @Test
    fun `Modifiers in a sentence`() {
        Grammy.setRandom(Random(15))

        val json = readJSON("modifiers.json")

        val grammar = Grammy.createGrammar(json)
        val expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("Purple unicorns are always indignant. A kitten is impassioned, unless it is an orange one. Truly!"))
    }

    @ParameterizedTest
    @MethodSource("modifierArgs")
    fun `Modifiers`(rule: String, modifierName: String, result: String) {
        Grammy.setRandom(Random(0))

        val grammar = Grammy.createGrammar()
        grammar.addSymbol("name", setOf(rule))
        grammar.addSymbol("origin", setOf("{name.$modifierName}"))

        assertThat(grammar.flatten(), `is`(result))
    }

    @Test
    fun `Modifier optional edge case`() {
        Grammy.setRandom(Random(0))

        val grammar = Grammy.createGrammar()
        grammar.addSymbol("name", setOf("name"))
        grammar.addSymbol("origin", setOf("{name.optional(0)}"))

        assertThat(grammar.flatten(), `is`(""))
    }

    @Test
    fun `Recursive`() {
        Grammy.setRandom(Random(15))

        val json = readJSON("recursive.json")

        val grammar = Grammy.createGrammar(json)
        val expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("The purple unicorn of the mountain is called Darcy"))
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("nested1.json", "nested2.json"))
    fun `Nested`(jsonFile: String) {

        Grammy.setRandom(Random(15))

        val json = readJSON(jsonFile)

        val grammar = Grammy.createGrammar(json)
        val expandedText = grammar.flatten("animal")

        assertThat(expandedText, either(`is`("zebra")).or(`is`("horse")))
    }

    @Test
    fun `Write to JSON`() {
        val json = readJSON("modifiers.json")

        val grammar = Grammy.createGrammar(json)
        val generated = grammar.toJSON()

        assertThat(generated.replace("\n", ""), `is`(json))
    }

    @Test
    fun `Actions`() {
        Grammy.setRandom(Random(5))

        val json = readJSON("action.json")
        val grammar = Grammy.createGrammar(json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("Izzi traveled with her pet raven. Izzi was never astute, for the raven was always too impassioned."))
    }

    @Test
    fun `Regex selection`() {
        for (i in 1..15) {
            Grammy.setRandom(Random(i.toLong()))

            val json = readJSON("regex.json")
            val grammar = Grammy.createGrammar(json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("cow")).or(`is`("sparrow")))
        }
    }

    @Test
    fun `Regex selection 2`() {
        for (i in 1..15) {
            Grammy.setRandom(Random(i.toLong()))

            val json = readJSON("regex2.json")
            val grammar = Grammy.createGrammar(json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("cow")).or(`is`("duck")))
        }
    }

    @Test
    fun `Regex selection 3`() {
        for (i in 1..15) {
            Grammy.setRandom(Random(i.toLong()))

            val json = readJSON("regex3.json")
            val grammar = Grammy.createGrammar(json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("coyote")).or(`is`("eagle")))
        }
    }

    @Test
    fun `Num keyword`() {
        Grammy.setRandom(Random(5))

        val json = readJSON("num.json")
        val grammar = Grammy.createGrammar(json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("There are 1568779487 ravens with 189533474 different colors, but mainly grey."))
    }

    @Test
    fun `Distribution in selection`() {
        for (i in 1..50) {
            Grammy.setRandom(Random(i.toLong() * 100))

            val json = readJSON("distribution.json")
            val grammar = Grammy.createGrammar(json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("unicorn")).or(`is`("raven")))
        }
    }

    @Test
    fun `Story`() {
        Grammy.setRandom(Random(99))

        val json = readJSON("story.json")
        val grammar = Grammy.createGrammar(json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("Krox was a great baker, and this song tells of her adventure. Krox baked bread, then she made croissants, then she went home to read a book."))

        assertThat(grammar.flatten(), `is`("Brick was a great warrior, and this song tells of his adventure. Brick defeated a giant, then he battled an ogre, then he went home to read a book."))
        assertThat(grammar.flatten(), `is`("Cheri was a great warrior, and this song tells of his adventure. Cheri battled a goblin, then he saved a village from a goblin, then he went home to read a book."))
        assertThat(grammar.flatten(), `is`("Zelph was a great warrior, and this song tells of their adventure. Zelph fought a witch, then they battled a giant, then they went home to read a book."))
        assertThat(grammar.flatten(), `is`("Jedoo was a great warrior, and this song tells of their adventure. Jedoo battled an ogre, then they defeated a golem, then they went home to read a book."))

        assertThat(grammar.flatten(), `is`("Zelph was a great baker, and this song tells of her adventure. Zelph iced a cake, then she decorated cupcakes, then she went home to read a book."))
    }

    // FORMAL DEFINITION TESTS

    @Test
    fun `A rule cannot be empty`() {
        val grammar = Grammy.createGrammar()

        assertThrows(TracerySyntaxException::class.java, {
            grammar.addSymbol("key", setOf(""))
        })
    }

    @Test
    fun `A symbol key cannot be empty`() {
        val grammar = Grammy.createGrammar()

        assertThrows(TracerySyntaxException::class.java, {
            grammar.addSymbol("", setOf("rule1", "rule2"))
        })
    }

    @Test
    fun `A symbol ruleset cannot be empty`() {
        val grammar = Grammy.createGrammar()

        assertThrows(TracerySyntaxException::class.java, {
            grammar.addSymbol("key", setOf())
        })
    }

    @Test
    fun `A symbol total rule distribution value does not exceed 100`() {
        val grammar = Grammy.createGrammar()

        assertThrows(TracerySyntaxException::class.java, {
            grammar.addSymbol("key", setOf("rule1(50)", "rule2(51)"))
        })
    }

    @Test
    fun `Fail if grammar has no starting symbol`() {
        val grammar = Grammy.createGrammar()

        assertThrows(TracerySyntaxException::class.java, {
            grammar.flatten()
        })
    }

    @Test
    fun `Fail if no matching regex`() {
        val grammar = Grammy.createGrammar()
        grammar.addSymbol("name", setOf("text"))
        grammar.addSymbol("origin", setOf("{name#...#}"))

        assertThrows(TracerySyntaxException::class.java, {
            grammar.flatten()
        })
    }

    private fun readJSON(fileName: String): String {
        return Files.readAllLines(Paths.get(javaClass.getResource(fileName).toURI())).joinToString("")
    }

    companion object {
        @JvmStatic fun modifierArgs(): List<Arguments> {
            return listOf(
                    // format:  expanded tag, modifier name, result
                    Arguments.of("text", "capitalizeAll", "TEXT"),
                    Arguments.of("text", "capitalize", "Text"),
                    Arguments.of("text", "s", "texts"),
                    Arguments.of("dish", "s", "dishes"),
                    Arguments.of("fix", "s", "fixes"),
                    Arguments.of("pass", "s", "passes"),
                    Arguments.of("ally", "s", "allies"),
                    Arguments.of("key", "s", "keys"),
                    Arguments.of("text", "a", "a text"),
                    Arguments.of("apple", "a", "an apple"),
                    Arguments.of("kill", "ed", "killed"),
                    Arguments.of("fire", "ed", "fired"),
                    Arguments.of("fry", "ed", "fried"),
                    Arguments.of("stay", "ed", "stayed")
            )
        }
    }
}