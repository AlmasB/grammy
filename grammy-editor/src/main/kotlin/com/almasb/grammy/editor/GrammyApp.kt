package com.almasb.grammy.editor

import com.almasb.grammy.Grammy
import javafx.application.Application
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.Stage

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class GrammyApp : Application() {

//    companion object {
//        private val VERBS = Files.readAllLines(Path.of(RandomStoryCreator::class.java.getResource("verbs.txt").toURI()))
//        private val NOUNS = Files.readAllLines(Path.of(RandomStoryCreator::class.java.getResource("nouns.txt").toURI()))
//        private val ADJECTIVES = Files.readAllLines(Path.of(RandomStoryCreator::class.java.getResource("adj.txt").toURI()))
//    }

    private fun createContent(): Parent {
        val root = VBox(10.0)
        root.setPrefSize(800.0, 600.0)

        val area1 = TextArea()
        area1.font = Font.font(38.0)
        area1.isWrapText = true
        area1.text = "{\n" +
                "  \"origin\": []\n" +
                "}"

        val area2 = TextArea()
        area2.font = Font.font(38.0)
        area2.isWrapText = true

        val btnRun = Button("Run")
        btnRun.setOnAction {
            val grammar = Grammy.createGrammar(area1.text)

//            grammar.addSymbol("noun", NOUNS)
//            grammar.addSymbol("verb", VERBS)
//            grammar.addSymbol("adj", ADJECTIVES)

            try {
                area2.text = grammar.flatten()
            } catch (e: Exception) {
                area2.text = "Syntax error: $e"
            }
        }

        val btnCreate = Button("Random Text")
        btnCreate.setOnAction {
            val creator = RandomStoryCreator()

            area1.text = creator.createStoryAsJSON()
        }

        val box = HBox(area1, area2)
        box.prefHeight = 550.0

        root.children.addAll(box, HBox(btnRun, btnCreate))

        return root
    }

    override fun start(stage: Stage) {
        stage.scene = Scene(createContent())
        stage.show()
    }
}

fun main() {
    Application.launch(GrammyApp::class.java)
}