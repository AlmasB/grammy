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

    @Test
    fun `Basic syntax 1`() {
        val json = readJSON("simple1.json")

        val grammar = Grammy.createGrammar(0, json)
        val expandedText = grammar.flatten("animal")

        assertThat(expandedText, `is`("unicorn raven"))
    }

    @Test
    fun `Basic syntax 2`() {
        val json = readJSON("simple2.json")

        val grammar = Grammy.createGrammar(0, json)
        val expandedText = grammar.flatten("randomAnimal")

        assertThat(expandedText, either(`is`("unicorn")).or(`is`("raven")))
    }

    @Test
    fun `Basic syntax 3`() {
        val json = readJSON("simple3.json")

        var grammar = Grammy.createGrammar(0, json)
        var expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("The purple owl of the river is called Chiaki"))

        grammar = Grammy.createGrammar(1, json)
        expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("The purple owl of the mountain is called Mia"))
    }

    @Test
    fun `The same symbol expands to random text`() {
        val json = readJSON("simple4.json")

        val grammar = Grammy.createGrammar(0, json)
        val expandedText = grammar.flatten("sentence")

        val tokens = expandedText.split(",")

        assertThat(tokens[0], `is`(not(tokens[1])))
    }

    @Test
    fun `Modifiers in a sentence`() {
        val json = readJSON("modifiers.json")

        val grammar = Grammy.createGrammar(15, json)
        val expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("Purple unicorns are always indignant. A kitten is impassioned, unless it is an orange one. Truly!"))
    }

    @ParameterizedTest
    @MethodSource("modifierArgs")
    fun `Modifiers`(rule: String, modifierName: String, result: String) {
        val grammar = Grammy.createGrammar()
        grammar.addSymbol("name", listOf(rule))
        grammar.addSymbol("origin", listOf("{name.$modifierName}"))

        assertThat(grammar.flatten(), `is`(result))
    }

    @Test
    fun `Modifier optional edge case`() {
        val grammar = Grammy.createGrammar()
        grammar.addSymbol("name", listOf("name"))
        grammar.addSymbol("origin", listOf("{name.optional(0)}"))

        assertThat(grammar.flatten(), `is`(""))
    }

    @Test
    fun `Recursive`() {
        val json = readJSON("recursive.json")

        val grammar = Grammy.createGrammar(15, json)
        val expandedText = grammar.flatten("sentence")

        assertThat(expandedText, `is`("The purple unicorn of the mountain is called Darcy"))
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("nested1.json", "nested2.json"))
    fun `Nested`(jsonFile: String) {
        val json = readJSON(jsonFile)

        val grammar = Grammy.createGrammar(15, json)
        val expandedText = grammar.flatten("animal")

        assertThat(expandedText, either(`is`("zebra")).or(`is`("horse")))
    }

    @Test
    fun `Write to JSON`() {
        val json = readJSON("modifiers.json")

        val grammar = Grammy.createGrammar(0, json)
        val generated = grammar.toJSON()

        assertThat(generated.replace("\n", ""), `is`(json))
    }

    @Test
    fun `Actions`() {
        val json = readJSON("action.json")
        val grammar = Grammy.createGrammar(5, json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("Izzi traveled with her pet raven. Izzi was never astute, for the raven was always too impassioned."))
    }

    @Test
    fun `Nested Actions`() {
        val json = readJSON("nested_actions.json")
        val grammar = Grammy.createGrammar(5, json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("1, 1, 2"))
    }

    @Test
    fun `Action without symbol notation`() {
        val json = readJSON("action_without_symbol.json")
        val grammar = Grammy.createGrammar(5, json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("Izzi traveled with her pet raven. Izzi was never courteous, for the raven was always too vexed and vexed."))
    }

    @Test
    fun `Add action at runtime`() {
        val json = readJSON("add_action_runtime.json")
        val grammar = Grammy.createGrammar(339, json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("Izzi traveled with her pet raven. Izzi was never originalMood2, or originalMood1, or vexed and originalMood1."))
    }

    @Test
    fun `Regex selection`() {
        for (i in 1..15) {
            val json = readJSON("regex.json")
            val grammar = Grammy.createGrammar(i.toLong(), json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("cow")).or(`is`("sparrow")))
        }
    }

    @Test
    fun `Regex selection 2`() {
        for (i in 1..15) {
            val json = readJSON("regex2.json")
            val grammar = Grammy.createGrammar(i.toLong(), json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("cow")).or(`is`("duck")))
        }
    }

    @Test
    fun `Regex selection 3`() {
        for (i in 1..15) {
            val json = readJSON("regex3.json")
            val grammar = Grammy.createGrammar(i.toLong(), json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("coyote")).or(`is`("eagle")))
        }
    }

    @Test
    fun `Num keyword`() {
        val json = readJSON("num.json")
        val grammar = Grammy.createGrammar(5, json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("There are 1568779487 ravens with 189533474 different colors, but mainly grey."))
    }

    @Test
    fun `Distribution in selection`() {
        for (i in 1..50) {
            val json = readJSON("distribution.json")
            val grammar = Grammy.createGrammar(i.toLong() * 100, json)
            val expansion = grammar.flatten("randomAnimal")

            assertThat(expansion, either(`is`("unicorn")).or(`is`("raven")))
        }
    }

    @Test
    fun `Story`() {
        val json = readJSON("story.json")
        val grammar = Grammy.createGrammar(99, json)
        val expansion = grammar.flatten()

        assertThat(expansion, `is`("Krox was a great baker, and this song tells of her adventure. Krox baked bread, then she made croissants, then she went home to read a book."))

        assertThat(grammar.flatten(), `is`("Brick was a great warrior, and this song tells of his adventure. Brick defeated a giant, then he battled an ogre, then he went home to read a book."))
        assertThat(grammar.flatten(), `is`("Cheri was a great warrior, and this song tells of his adventure. Cheri battled a goblin, then he saved a village from a goblin, then he went home to read a book."))
        assertThat(grammar.flatten(), `is`("Zelph was a great warrior, and this song tells of their adventure. Zelph fought a witch, then they battled a giant, then they went home to read a book."))
        assertThat(grammar.flatten(), `is`("Jedoo was a great warrior, and this song tells of their adventure. Jedoo battled an ogre, then they defeated a golem, then they went home to read a book."))

        assertThat(grammar.flatten(), `is`("Zelph was a great baker, and this song tells of her adventure. Zelph iced a cake, then she decorated cupcakes, then she went home to read a book."))
    }

    @Test
    fun `Modify symbols at runtime with add and remove`() {
        val json = readJSON("add_remove_symbols.json")
        val grammar = Grammy.createGrammar(json)

        assertThat(grammar.flatten(), `is`("delighted"))
    }

    // FORMAL DEFINITION TESTS

    @Test
    fun `A rule cannot be empty`() {
        val grammar = Grammy.createGrammar()

        assertThrows(GrammySyntaxException::class.java, {
            grammar.addSymbol("key", listOf(""))
        })
    }

    @Test
    fun `A symbol key cannot be empty`() {
        val grammar = Grammy.createGrammar()

        assertThrows(GrammySyntaxException::class.java, {
            grammar.addSymbol("", listOf("rule1", "rule2"))
        })
    }

    @Test
    fun `A symbol ruleset cannot be empty`() {
        val grammar = Grammy.createGrammar()

        assertThrows(GrammySyntaxException::class.java, {
            grammar.addSymbol("key", listOf())
        })
    }

    @Test
    fun `A symbol total rule distribution value does not exceed 100`() {
        val grammar = Grammy.createGrammar()

        assertThrows(GrammySyntaxException::class.java, {
            grammar.addSymbol("key", listOf("rule1(50)", "rule2(51)"))
        })
    }

    @Test
    fun `Fail if grammar has no starting symbol`() {
        val grammar = Grammy.createGrammar()

        assertThrows(GrammySyntaxException::class.java, {
            grammar.flatten()
        })
    }

    @Test
    fun `Fail if no matching regex`() {
        val grammar = Grammy.createGrammar()
        grammar.addSymbol("name", listOf("text"))
        grammar.addSymbol("origin", listOf("{name#...#}"))

        assertThrows(GrammySyntaxException::class.java, {
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