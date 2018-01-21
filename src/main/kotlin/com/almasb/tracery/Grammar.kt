package com.almasb.tracery

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.HashMap



/**
 *
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class Grammar(val symbols: Map<String, Array<String>> = hashMapOf()) {

    fun fromJSON(json: String) {
        val typeRef = object : TypeReference<HashMap<String, Array<String>>>() {}

        val map: Map<String, Array<String>> = jacksonObjectMapper().readValue(json, typeRef)
        symbols.toMutableMap().putAll(map)

//        println(map.values.flatMap { it.asIterable() })
//        println(symbols.values.flatMap { it.asIterable() })
    }

    fun toJSON(): String {
        return jacksonObjectMapper().writeValueAsString(symbols)
    }
}