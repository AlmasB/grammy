package com.almasb.grammy.core;

import java.util.Random;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public final class Grammy {

    // this might be needed in the future to preserve the state
    private static final Grammy instance = new Grammy();

    private Grammy() {}

    static Random random = new Random();

    /**
     * Set random number generator to make Grammy deterministic.
     */
    public static void setRandom(Random random) {
        Grammy.random = random;
    }

    /**
     * Create empty grammar.
     */
    public static Grammar createGrammar() {
        return new Grammar();
    }

    /**
     * Create grammar from a JSON string in Grammy format.
     */
    public static Grammar createGrammar(String json) {
        Grammar grammar = createGrammar();
        grammar.fromJSON(json);
        return grammar;
    }
}
