---
layout: page
title: StatefulJ FSM
---

The *StatefulJ FSM* is a lightweight *Finite State Machine* with support for *Deterministic and Non-Determinstic Transitions*.  Stateful FSM is self-contained with minimal dependencies (just SLF4J for a logging facade).

## Installation

Install StatefulJ FSM from Maven Central into your app by adding the following to your pom.xml:

```xml
<dependency>
	<groupId>org.statefulj</groupId>
	<artifactId>statefulj-fsm</artifactId>
	<version>{{ site.version }}</version>
</dependency>
```

Or if you are feeling adventurous, you can [download and build the latest from source](https://github.com/statefulj/statefulj). 

## Coding

To use StatefulJ FSM, you build *State Models*.  A State Model is a set of *States* and *Transitions* which is associated with a Class.  This Class is referred to as the *Stateful Entity*.  

To create a State Model, you will need to:

* [Define your *Stateful Entity*](#define-your-stateful-entity)
* [Define your *Events*](#define-your-events)
* [Define your *States*](#define-your-states)
* [Define your *Actions*](#define-your-actions)
* [Define your *Transitions*](#define-your-transitions)
* [Define your *Persister*](#define-your-persister)
* [Construct the *FSM*](#construct-the-fsm)

### Define your Stateful Entity

A *Stateful Entity* is a class that contains a *State* field which is managed by *StatefulJ FSM*.  The type of the State Field is dependent on the *Persister* being used. The State field is defined by a *@State* annotation.

```java

// Stateful Entity
//
public class Foo {

	@State
	String state;   // Memory Persister requires a String
	
	boolean bar;
	
	public String getState() {
		return state;
	}
	
	// Note: there is no setter for the state field 
	//       as the value is set by StatefulJ
	
	public void setBar(boolean bar) {
		this.bar = bar;
	}
	
	public boolean isBar() {
		return bar;
	}
	
}
```

```java
// Instantiate the Stateful Entity
//
Foo foo = new Foo();
```

### Define your Events

*Events* in StatefulJ are Strings.

```java
// Events
//
String eventA = "Event A";
String eventB = "Event B";
```

### Define your States

A State defines the state value for an Entity and holds the mapping of all Transitions for that State.

```java		
// States
//
StateImpl<Foo> stateA = new StateImpl<Foo>("State A");
StateImpl<Foo> stateB = new StateImpl<Foo>("State B");
StateImpl<Foo> stateC = new StateImpl<Foo>("State C", true); // End State
```
		
### Define your Actions

An *Action* is a *Command* object.

```java
// Hello <what> Action
//
public class HelloAction<T> implements Action<T> {

	String what;
	
	public HelloAction(String what) {
		this.what = what;
	}

	public void execute(T stateful, 
	                    String event, 
	                    Object ... args) throws RetryException {
		System.out.println("Hello " + what);
	}	
}
```		

```java

// Actions
//
Action<Foo> actionA = new HelloAction("World");
Action<Foo> actionB = new HelloAction("Folks");
```

### Define your Transitions

A *Transition* is a reaction to an *Event* directed at a *Stateful Entity*.  The *Transition* can involve a possible change in *State* and a possible *Action*.  

Transitions are referred as being either *Deterministic* or *Non-Deterministic*:

* A Deterministic Transition means that for a given State and Event, there is only a single Transition. 
* A Non-Deterministic Transition means that for a given State and Event there is more than one Transition.

Transitions are added to a State and are mapped by an Event.

#### Deterministic Transitions

```java
/* Deterministic Transitions */

// stateA(eventA) -> stateB/actionA
//
stateA.addTransition(eventA, stateB, actionA); 
	
// stateB(eventB) -> stateC/actionB
//
stateB.addTransition(eventB, stateC, actionB);
```

#### Non-Deterministic Transitions

```java
/* Non-Deterministic Transitions */

//                   +--> stateB/NOOP  -- loop back on itself
//  stateB(eventA) --|
//                   +--> stateC/NOOP
//
stateB.addTransition(eventA, new Transition<Foo>() {
	
	public StateActionPair<Foo> getStateActionPair(Foo stateful) {
		State<Foo> next = null;
		if (stateful.isBar()) {
			next = stateB;
		} else {
			next = stateC;
		}
		
		// Move to the next state without taking any action
		//
		return new StateActionPairImpl<Foo>(next, null);
	}
});
```

```
		// FSM
		//
		MemoryPersisterImpl<Foo> persister = new MemoryPersisterImpl<Foo>(stateful, stateA);
		FSM<Foo> fsm = new FSM<Foo>("Foo FSM", persister);

		// Verify that on eventA, we transition to StateB and verify
		// that we call actionA with the correct arg
		//
		Object arg = new Object();
		State<Foo> current = fsm.onEvent(stateful, eventA, arg);
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