### About
This is a JVM port of Kate Compton's story-grammar generator - [Tracery](https://github.com/galaxykate/tracery) 

![Maven](https://img.shields.io/maven-central/v/com.github.almasb/grammy.svg)
[![Build Status](https://travis-ci.org/AlmasB/grammy.svg?branch=master)](https://travis-ci.org/AlmasB/grammy)
[![codecov](https://codecov.io/gh/AlmasB/grammy/branch/master/graph/badge.svg)](https://codecov.io/gh/AlmasB/grammy)

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
Grammar grammar = Grammy.createGrammar(json);

String output = grammar.flatten("emotion");

// output is one of "happy", "sad" or "proud"
```

#### Example 2

Input:

```json
{
  "sentence": ["The {color} {animal} of the {natureNoun} is called {name}"],
  "color": ["orange","blue","white","black","grey","purple","indigo","turquoise"],
  "animal": ["unicorn","raven","sparrow","coyote","eagle","owl","zebra","duck","kitten"],
  "natureNoun": ["ocean","mountain","forest","cloud","river","tree","sky","sea","desert"],
  "name": ["Arjun","Yuuma","Darcy","Mia","Chiaki","Izzi","Azra","Lina"]
}
```

Code:

```
Grammar grammar = ...
grammar.flatten("sentence");
```

Possible Output:

```
The orange zebra of the sky is called Mia
```

#### Example 3

Input:

```json
{
  "origin" : ["{[helper:{name}]story}"],
  "story" : ["{greet.capitalize}! {introduce.optional(75)} {ask} {goodbye}", "{ask} {goodbye}"],
  "greet" : ["hi", "hello", "hi there", "hey", "good day"],
  "introduce" : ["My name is {helper.capitalize}. I'm your {helperJob} for today.", "The name's {helper.capitalize}. How do you do? I'll be your {helperJob} today.", "I'm {helper.capitalize} and I'm your {helperJob}."],
  "helperJob" : ["{jobType.optional(50)} assistant"],
  "jobType" : ["digital", "virtual"],
  "ask" : ["How {can} I help you?", "How {can} I be of service?", "What {can} I do for you?"],
  "can" : ["can", "may", "might"],
  "name" : ["Alice", "Bob", "Carl", "David", "Eve", "Francis", "Gerard", "Helen", "Ian", "Jenny"],
  "goodbye" : ["You can ask for {helper} next time you need help. {bye.capitalize}!"],
  "bye" : ["bye", "goodbye", "until next time", "see you", "take care"]
}
```

Code:

```
Grammar grammar = ...
grammar.flatten();
```

While the possible outputs follow the same (defined) structure, there's a great variety in what can be achieved:

```
Good day! My name is Helen. I'm your virtual assistant for today. What may I do for you? You can ask for Helen next time you need help. Take care!
Hey! How can I be of service? You can ask for Ian next time you need help. Bye!
Hi! The name's Helen. How do you do? I'll be your digital assistant today. What may I do for you? You can ask for Helen next time you need help. Goodbye!
How can I help you? You can ask for Eve next time you need help. Until next time!
What can I do for you? You can ask for Eve next time you need help. Until next time!
Good day! How may I help you? You can ask for Eve next time you need help. Goodbye!
Hi there! The name's Helen. How do you do? I'll be your virtual assistant today. What may I do for you? You can ask for Helen next time you need help. Take care!
How can I help you? You can ask for Bob next time you need help. Goodbye!
Hello! My name is Francis. I'm your digital assistant for today. How might I help you? You can ask for Francis next time you need help. Bye!
What can I do for you? You can ask for Jenny next time you need help. See you!
What can I do for you? You can ask for Helen next time you need help. Goodbye!
Hey! My name is Francis. I'm your virtual assistant for today. How can I be of service? You can ask for Francis next time you need help. See you!
How can I be of service? You can ask for Eve next time you need help. Goodbye!
What might I do for you? You can ask for David next time you need help. Take care!
How can I help you? You can ask for Francis next time you need help. Bye!
How can I be of service? You can ask for Alice next time you need help. See you!
Good day! I'm Helen and I'm your assistant. How can I help you? You can ask for Helen next time you need help. Until next time!
How may I be of service? You can ask for Francis next time you need help. See you!
How might I be of service? You can ask for Ian next time you need help. Until next time!
Hi there! How can I be of service? You can ask for Carl next time you need help. See you!
Hi there! My name is Alice. I'm your assistant for today. How might I help you? You can ask for Alice next time you need help. Take care!
How may I help you? You can ask for Alice next time you need help. Goodbye!
Hey! The name's Bob. How do you do? I'll be your virtual assistant today. How can I be of service? You can ask for Bob next time you need help. Until next time!
What may I do for you? You can ask for Gerard next time you need help. See you!
Hello! The name's Alice. How do you do? I'll be your digital assistant today. How may I be of service? You can ask for Alice next time you need help. Until next time!
```

#### Actions

Actions are a way to create and modify symbols at runtime.

---

The `!` prefix means overwrite.

Input:

```json
{
  "origin" : ["[name:Bob][!name:Adam]{story}"],
  "story" : ["{name}"]
}
```

Output:

```
Adam
```

---

The `+` prefix (or without a prefix) means add.

Input:

```json
{
  "origin" : ["[name:Bob][name:+Adam]{story}"],
  "story" : ["{name}"]
}
```

Possible outputs are:

```
Adam
Bob
```

---

The `-` prefix means remove.

Input:

```json
{
  "origin" : ["[name:Bob,Adam][name:-Adam]{story}"],
  "story" : ["{name}"]
}
```

Output:

```
Bob
```

### Maven

```
<dependency>
    <groupId>com.github.almasb</groupId>
    <artifactId>grammy</artifactId>
    <version>0.0.2</version>
</dependency>
```

### Gradle

```
compile 'com.github.almasb:grammy:0.0.2'
```

### Uber-jar

Pre-compiled version available from [Releases](https://github.com/AlmasB/grammy/releases).

### Notes

This implementation only loosely follows the [original specification](https://github.com/galaxykate/tracery/tree/tracery2) by Kate Compton.
So, given the same data, the output may differ.

The word list is used from [wordlist](https://github.com/aaronbassett/Pass-phrase).

### Contact

![Gmail](https://img.shields.io/badge/email-almaslvl@gmail.com-red.svg)