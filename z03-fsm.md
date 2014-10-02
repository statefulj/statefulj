---
layout: page
title: StatefulJ FSM
---

The *StatefulJ FSM* library is a dependency free, Finite State Machine implementation with support for *Non-Determinstic Transitions*.

## Installation

Include the FSM jar into your Maven build:

```xml
<dependency>
	<groupId>org.statefulj</groupId>
	<artifactId>statefulj-fsm</artifactId>
	<version>{{ site.version }}</version>
</dependency>
```

Or if you are feeling adventurous, you can [download and build the latest from source](https://github.com/statefulj/statefulj). 

## Coding

With StatefulJ, you will define a Finite State Machine for each State Model.  A State Model is associated with a give Class which is referred to as the Stateful Entity.  So Within your code, you will need to:

* Define your *Stateful Entity*
* Define your *Events*
* Define your *States*
* Define your *Actions*
* Define your *Transitions*
* Construct the *FSM*

### Define your Stateful Entity


```java
// Stateful Entity
//
Foo foo = new Foo();
```

### Define your Events

*Events* in StatefulJ are simple Strings.  Example:

```java
// Events
//
String eventA = "eventA";
String eventB = "eventB";
```

### Define your States

A State defines the state value for an Entity, as well as, hold the mapping of all Transitions for a State.

```java		
// States
//
StateImpl<Foo> stateA = new StateImpl<Foo>("stateA");
StateImpl<Foo> stateB = new StateImpl<Foo>("stateB");
StateImpl<Foo> stateC = new StateImpl<Foo>("stateC", true); // End State
```
		
### Define your Transitions

A *Transition* is a reaction to an *Event* directed at a *Stateful Entity*.  The *Transition* can involve a possible change in *State* and a possible *Action*.  

Transitions are referred as being either *Determinstic* or *Non-Deterministic*.  A Deterministic Transition means that for a given State and Event, there is only a single Transition. A Non-Deterministic Transition means that for a given State and Event there is more than one Transition.  In StatefulJ

```java
// Deterministic Transitions
//
stateA.addTransition(
	eventA, 
	new DeterministicTransitionImpl<Object>(stateB, actionA));
	
stateB.addTransition(
	eventB, 
	new DeterministicTransitionImpl<Object>(stateC, actionB));
```

```
		// FSM
		//
		MemoryPersisterImpl<Object> persister = new MemoryPersisterImpl<Object>(stateful, stateA);
		FSM<Object> fsm = new FSM<Object>("SimpleFSM", persister);

		// Verify that on eventA, we transition to StateB and verify
		// that we call actionA with the correct arg
		//
		Object arg = new Object();
		State<Object> current = fsm.onEvent(stateful, eventA, arg);
		assertEquals(stateB, current);
		assertFalse(current.isEndState());
		verify(actionA).execute(stateful, eventA, arg);
		verify(actionB, never()).execute(stateful, eventA, arg);
		
		reset(actionA);
		
		// Verify that on eventA from StateB, that nothing happened
		//
		current = fsm.onEvent(stateful, eventA, arg);
		assertEquals(stateB, current);
		assertFalse(current.isEndState());
		verify(actionA, never()).execute(stateful, eventA, arg);
		verify(actionB, never()).execute(stateful, eventA, arg);
		
		// Verify that on eventB from StateB, we transition to stateC - the endState, and
		// call actionB with the correct args
		//
		current = fsm.onEvent(stateful, eventB, arg);
		assertEquals(stateC, current);
		assertTrue(current.isEndState());
		verify(actionA, never()).execute(stateful, eventB, arg);
		verify(actionB).execute(stateful, eventB, arg);
		
```