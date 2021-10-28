package com.almasb.grammy;

import java.util.Random;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public final class Grammy {

    private Grammy() {}

    /**
     * Create empty grammar with given seed.
     */
    public static Grammar createGrammar(long seed) {
        return new Grammar(new Random(seed));
    }

    /**
     * Create empty grammar.
     */
    public static Grammar createGrammar() {
        return new Grammar();
    }

    /**
     * Create grammar from a JSON string in Grammy format with given seed.
     */
    public static Grammar createGrammar(long seed, String json) {
        Grammar grammar = createGrammar(seed);
        grammar.fromJSON(json);
        return grammar;
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
