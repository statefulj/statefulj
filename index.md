---
layout: page
title: Introduction
weight: 0
---

## What is StatefulJ?

[StatefulJ](http://www.statefulj.org) is both an lightweight, open source Java Finite State Machine(FSM) and a complete Spring based framework which lets you easily define and integrate State Machines into your Applications.

## What is a Finite State Machine?

I won't go into detail here as there is [a lot already written about them](http://en.wikipedia.org/wiki/Finite-state_machine).  But the important thing is that an FSM allows you to *model all your Domain events and State transitions into a coherent and testable package* instead of writing a scattershot of boolean flags and if-then statements. 

## Why should I use StatefulJ?

Modern day applications have to manage and orchestrate requests and events from many sources: REST and SOAP APIs, Web Page requests, Message Queues, Internal Application Events, etc...  Handling concurrency and reliably updating Application state becomes a major challenge without the proper framework.  

By making easy to integrate State Machines into your Application, *StatefulJ* provides you the ability to meet these challenges by creating *State Models* instead of writing ad-hoc code.  

## What does StatefulJ provide?

StatefulJ provides the following "packages":

* **StatefulJ FSM:** A dependency free, Finite State Machine implementation with support for non-determinstic Transitions.
* **StatefulJ Persistence:** A set of persistence support (JPA and Mongo) which works with the FSM library to persist your Stateful objects.
* **StatefulJ Framework:** A framework built off StatefulJ FSM, StatefulJ Persistence and Spring Data to easily integrate State Machines into your Application.

## How do I get started?

If you just want the Core FSM, then follow these instructions

If you want to use the framework, then go here

