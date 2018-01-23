### About
This is a JVM port of Kate Compton's story-grammar generator - [Tracery](https://github.com/galaxykate/tracery) 

[![Build Status](https://travis-ci.org/AlmasB/tracery.svg?branch=master)](https://travis-ci.org/AlmasB/tracery)
[![codecov](https://codecov.io/gh/AlmasB/tracery/branch/master/graph/badge.svg)](https://codecov.io/gh/AlmasB/tracery)

### Usage

#### Example 1

Given the following JSON:

```json
{
  "emotion" : ["happy", "sad", "proud"]
}
```

We can create `Grammar` as follows:

```
String json = ...
Grammar grammar = Tracery.createGrammar(json);

String output = grammar.flatten("#emotion#");

// output is one of "happy", "sad" or "proud"
```

#### Example 2

Input:

```json
{
  "sentence": ["The #color# #animal# of the #natureNoun# is called #name#"],
  "color": ["orange","blue","white","black","grey","purple","indigo","turquoise"],
  "animal": ["unicorn","raven","sparrow","coyote","eagle","owl","zebra","duck","kitten"],
  "natureNoun": ["ocean","mountain","forest","cloud","river","tree","sky","sea","desert"],
  "name": ["Arjun","Yuuma","Darcy","Mia","Chiaki","Izzi","Azra","Lina"]
}
```

Code:

```
Grammar grammar = ...
grammar.flatten("#sentence#");
```

Possible Output:

```
The orange zebra of the sky is called Mia
```

### Notes

This implementation only loosely follows the [original specification](https://github.com/galaxykate/tracery/tree/tracery2) by Kate Compton.
So, given the same data, the output may differ.

### Contact

![Gmail](https://img.shields.io/badge/email-almaslvl@gmail.com-red.svg)