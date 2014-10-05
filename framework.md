---
layout: page
title: StatefulJ Framework
---

The *StatefulJ Framework* leverages *StatefulJ FSM*, *StatefulJ Persister* and Spring Data to seamlessly integrate
StatefulJ into your Application.  Using Annotations to define your State Model, StatefulJ will auto generate the FSM 
infrastructure, bind with with *Endpoint Providers* and manage all State Persistence.

## Installation

Installing *StatefulJ Framework* is dependent on the technologies within your stack.  The StatefulJ Frameworks is comprised of *Binders* and *Persisters*.  

Binders "bind" *Endpoint Providers* to the StatefulJ Framework.  StatefulJ supports the following Endpoint Providers:

* SpringMVC
* Camel
* Jersey

Persisters interacts within the underlying databases.  StatefulJ supports the following Persisters:

* JPA (SQL)
* MongoDB

So, depending on your stack, you will need to include the following dependencies into your Maven project:

### Binders

#### SpringMVC

```xml
<dependency>
	<groupId>org.statefulj.framework</groupId>
	<artifactId>statefulj-framework-binders-springmvc</artifactId>
	<version>{{ site.version }}</version>
</dependency>
```

#### Camel

```xml
<dependency>
	<groupId>org.statefulj.framework</groupId>
	<artifactId>statefulj-framework-binders-camel</artifactId>
	<version>{{ site.version }}</version>
</dependency>
```

#### Jersey

```xml
<dependency>
	<groupId>org.statefulj.framework</groupId>
	<artifactId>statefulj-framework-binders-jersey</artifactId>
	<version>{{ site.version }}</version>
</dependency>
```

### Persisters

#### JPA

```xml
<dependency>
	<groupId>org.statefulj.framework</groupId>
	<artifactId>statefulj-framework-persistence-jpa</artifactId>
	<version>{{ site.version }}</version>
</dependency>
```

#### Mongo

```xml
<dependency>
	<groupId>org.statefulj.framework</groupId>
	<artifactId>statefulj-framework-persistence-mongo</artifactId>
	<version>{{ site.version }}</version>
</dependency>
```

## Coding

To integrate the *StatefulJ Framework*, you define your *State Model*.  To create a State Model, you will need to:

* [Define your *Stateful Entity*](#define-your-stateful-entity)
* [Define your *States*](#define-your-states)
* [Define your *StateController*] (#define-your-controller)
* [Define your *Events*](#define-your-events)
* [Define your *Transitions*](#define-your-transitions)
* [Define your *Actions*](#define-your-actions)

### <a name="define-your-stateful-entity"></a> Define your Stateful Entity

A *Stateful Entity* is a Class that is persisted, managed by Spring Data and contains a *State* field.  The State Field defines the current State of the Entity and is managed by *StatefulJ Framework*.  The state field is annotated with the [@State](/public/javadoc/org/statefulj/persistence/annotations/State.html) annotation.  For your convenience, you can inherit from either the [*StatefulEntity* Class (JPA)](/public/javadoc/org/statefulj/persistence/jpa/model/StatefulEntity.html) or [*StatefulDocument* Class (Mongo)](/public/javadoc/org/statefulj/persistence/mongo/model/StatefulDocument.html).  This will automatically define a state field annotated with the [@State](/public/javadoc/org/statefulj/persistence/annotations/State.html) Annotation.

#### Stateful Entity (JPA)

```java

// Stateful Entity
//
@Entity
public class Foo extends StatefulEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	Long id;	

	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
}	
```

#### Stateful Document (Mongo)

```java

// Stateful Document
//
@Document
public class Foo extends StatefulDocument {
	
	@Id
	String id;	

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
}	
```

**Note:** StatefulJ Framework determines the type of Persister to use for an Entity by the Entity's association with a SpringData Repository.  As such, you will need to define a Repository like so:

```java
public interface FooRepository extends JpaRepository<Foo, Long> {
	
}
``` 

```java
public interface FooRepository extends MongoRepository<Foo, String> {
	
}
``` 

### <a name="define-your-states"></a> Define your States

A *State* defines the state value for an Entity and is of type *String*.  It is recommended that you define your States as public static constants within your Stateful Entity Class.

```java

// Stateful Entity
//
@Entity
public class Foo extends StatefulEntity {
	
	// States
	//
	public static final String NON_EXISTENT = "Non Existent";
	public static final String STATE_A = "State A";
	public static final String STATE_B = "State B";
	public static final String STATE_C = "State C";

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	Long id;	

	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
}	
```
		
### <a name="define-your-controller"></a> Define your Stateful Controller

*Stateful Controller* is the Class that defines the *State Model*.  It is defined by annotating the 
Class with the [@StatefulController Annotation](/public/javadoc/org/statefulj/framework/core/annotations/StatefulController.html).  The @StatefulContoller defines the *Stateful Entity* the Controller is managing.

```java
@StatefulController(
	clazz=Foo.class,
	startState=NON_EXISTENT
)
public class FooController {
}
```

The @StatefulController *must* define the Stateful Entity Class and the [*Start State*](http://en.wikipedia.org/wiki/Finite-state_machine#Start_state) for the Stateful Entity.

### <a name="define-your-actions"></a> Define your Actions

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

### <a name="define-your-transitions"></a> Define your Transitions

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

### <a name="define-your-persister"></a>Define your Persister

A *Persister* is a Class Responsible for persisting the State value for a Stateful Entity.  A Persister implements the 
Persister interface and *must* ensure that updates are atomic, isolated and thread-safe.  The *Stateful FSM* library comes with an
in-memory Persister which maintains the State only on the in-memory *Stateful Entity*.  If you need to persist to a database, you will
need to use one of the Database Persisters or integrate the *StatefulJ Framework*.

```java
// In-Memory Persister
//
List<State<Foo>> states = new LinkedList<State<Foo>>();
states.add(stateA);
states.add(stateB);
states.add(stateC);

MemoryPersisterImpl<Foo> persister = 
					new MemoryPersisterImpl<Foo>(
											states,   // Set of States 
											stateA);  // Start State
```

### <a name="construct-the-fsm"></a>Construct the FSM

The final step is construct the *FSM*.

```java
// FSM
//
FSM<Foo> fsm = new FSM<Foo>("Foo FSM", persister);

```
### <a name="using-the-fsm"></a>Using the FSM

Now that you have everything set up, you can drive your FSM by calling the *onEvent* method, passing in the *Stateful Entity* and the *Event*


```java
// Instantiate the Stateful Entity
//
Foo foo = new Foo();

// Drive the FSM with a series of events: eventA, eventA, eventA
//
fsm.onEvent(foo, eventA);  // stateA(EventA) -> stateB/actionA

foo.setBar(true);

fsm.onEvent(foo, eventA);  // stateB(EventA) -> stateB/NOOP

foo.setBar(false);

fsm.onEvent(foo, eventA);  // stateB(EventA) -> stateC/NOOP
```
