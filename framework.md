---
layout: page
title: StatefulJ Framework
---

The *StatefulJ Framework* leverages [*StatefulJ FSM*](/fsm), [*StatefulJ Persister*](/fsm#define-your-persister) and [Spring Data](http://projects.spring.io/spring-data/) to seamlessly integrate
StatefulJ into your Application.  Using Annotations to define your State Model, StatefulJ will auto generate the FSM 
infrastructure, bind with with *Endpoint Providers* and manage all State Persistence.

## <a href="#installation"></a> Installation

Installing *StatefulJ Framework* is dependent on the technologies within your stack.  The StatefulJ Frameworks is comprised of *Binders* and *Persisters*.  

Binders "bind" *Endpoint Providers* to the StatefulJ Framework.  The Binders are responsible for forwarding incoming requests from the Endpoint Providers as events along with the accompanying input.  The StatefulJ Framework supports the following Endpoint Providers:

* SpringMVC
* Jersey
* Camel

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

#### Jersey

```xml
<dependency>
	<groupId>org.statefulj.framework</groupId>
	<artifactId>statefulj-framework-binders-jersey</artifactId>
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

* [Instantiate the *Stateful Factory*](#instantiate-stateful-factory)
* [Define your *Stateful Entity*](#define-your-stateful-entity)
* [Define your *States*](#define-your-states)
* [Define your *StateController*] (#define-your-controller)
* [Define your *Events*](#define-your-events)
* [Define your *Transitions*](#define-your-transitions)
* [Inject the *StatefulFSM*](#inject-stateful-fsm)

### <a name="instantiate-stateful-factory"></a> Instantiate the Stateful Factory

The *Stateful Factory* will inspect the [StateControllers](#define-your-controller), bind with with *Endpoint Providers* and construct the *State Model*.  In order for the Stateful Factory to discover the [StateControllers](#define-your-controller), you must ensure that you have correctly defined your [Spring Component Scans](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html#beans-scanning-autodetection) to include the StateController's package.

#### XML Configuration

```xml
 	<bean id="statefulJFactory" class="org.statefulj.framework.core.StatefulFactory" />
```

#### Java Configuration

```java
	@Bean
	public StatefulFactory statefulJFactory() {
		return new StatefulFactory();
	}
```

#### Jersey Binding

For Jersey Binding, you must initialize an instance of StatefulJResourceConfig by extending StatefulJResourceConfig and mapping a root path.

```java
@ApplicationPath("/ajax")
public class JerseyConfig extends StatefulJResourceConfig {
	
}
```

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
	public static final String ACTIVE = "Active";
	public static final String UPGRADE_PENDING = "Upgrade Pending";
	public static final String UPGRADED = "Upgraded";

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

*Stateful Controller* defines the set of *Transitions* for the *Stateful Entity*.  It is defined by annotating the 
Class with the [@StatefulController Annotation](/public/javadoc/org/statefulj/framework/core/annotations/StatefulController.html).  The @StatefulContoller defines the *Stateful Entity* the Controller is managing.

| field     	|   Type    | Description |
|-----      	|-----    	|----     |
| clazz			| Required 	| The Entity class managed by the StatefulController |
| startState	| Required	|  The Starting State value.  If there is a transition from this State, the framework will pass in a new instance of the Managed Entity.  It is the responsibility of the StatefulController to persist the new instance. |
| value     	| Optional	| The value may indicate a suggestion for a logical component name, to be turned into a Spring bean in case of an autodetected component. |
| stateField	| Optional	| The name of the managed State field.  If blank, the Entity will be inspected for a field annotated with [State Annotation](/public/javadoc/org/statefulj/persistence/annotations/State.html) |
| factoryId		| Optional	| The bean Id of the Factory for this Entity.  The Factory Class must implement the [Factory Interface](/public/javadoc/org/statefulj/framework/core/model/Factory.html).  If not specified, the *StatefulJ Framework* will use the default Factory Implementation. |
| finderId 		| Optional	| The bean Id of the Finder for this Entity.  The Finder Class must implement the [Finder Interface](/public/javadoc/org/statefulj/framework/core/model/Finder.html).  If not specified, the *StatefulJ Framework* will use the default Finder Implementation. |
| persisterId	| Optional	| The bean Id of the Persister for this Entity.  The Persister is responsible for updating the State field for the Stateful Entity.  The Persister must implement the [Persister Interface](/public/javadoc/org/statefulj/fsm/Persister.html).  If not specified, the *StatefulJ Framework* will use the default Persister Implementation. |
| blockingStates | Optional	| Defines the set of "Blocking" States.  A Blocking State is a State that "block" an event from being handled until the FSM transitions out of the Blocking State |
| noops			| Optional	| An array of NOOP Transitions.  These Transitions will update the State field but will not invoke any Actions |

```java
@StatefulController(
	clazz=Foo.class,
	startState=NON_EXISTENT
)
public class FooController {
}
```

### <a name="define-your-events"></a> Define your Events

An *Event* is simply a String that directs the *StatefulJ Framework* how to bind an *Endpoint* to the Framework.  The format of the Event is &lt;binder&gt;:&lt;event&gt;

| Binder	|Format 																| Description 	|
|---		|-----------------------------------------------------------------------|---	      	|
| <none> | &lt;event&gt;	| Identifies an event that isn't bound to an Endpoint.  It is invoked directly from a [StatefulFSM](#inject-stateful-fsm) reference 
| SpringMVC | springmvc:&lt;get&verbar;post&verbar;patch&verbar;put&gt;:&lt;uri&gt;	| SpringMVC events require an *http verb* and a *uri*.  If the verb isn't specfied, it will default to a GET.  The uri must include an identifier for the Entity denoted by {id}, eg. springmvc:post:/foo/{id}/eventA|
| Jersey | jersey:&lt;get&verbar;post&verbar;patch&verbar;put&gt;:&lt;uri&gt;	| Jersey events require an *http verb* and a *uri*.  If the verb isn't specfied, it will default to a GET.  The uri must include an identifier for the Entity denoted by {id}, eg. jersey:post:/foo/{id}/eventA|
| Camel     | camel:&lt;route&gt; 												    | Camel events map to a route.  Since routes are typically not resource oriented, you will have to annotate a field in the *Message* with an [@Id](http://docs.spring.io/spring-data/commons/docs/current/api/index.html?org/springframework/data/domain/Persistable.html) annotation indicating the ID of the Entity | 

```java
@StatefulController(
	clazz=Foo.class,
	startState=NON_EXISTENT
)
public class FooController {

	// Events
	//
	public static final String CREATE_FOO = "springmvc:post:/foo";
	public static final String GET_FOO = "springmvc:get:/foo/{id}";
	public static final String UPGRADE_REQUEST = 
								"springmvc:post:/foo/{id}/upgrade";
	public static final String UPGRADE_APPROVED = "upgrade.approved";
	

}
```

### <a name="define-your-transitions"></a> Define your Transitions

A *Transition* is a reaction to an *Event* directed at a *Stateful Entity*.  The *Transition* can involve a possible change in *State* and a possible *Action*.  

In the *Stateful Framework*, a Transition is a method in the *Stateful Controller* annotated with the [@Transition](public/javadoc/org/statefulj/framework/core/annotations/Transition.html) annotation.  

| Field	    |Value 				         	| Description                                                |
|---		|-------------------         	| -------------                                                                                        |
| from      | &lt;state&gt;&nbsp;or&nbsp;&ast;	| The "from" State.  If left blank or the state is "&ast;", then this transition applies to all states |
| event		| &lt;event&gt;				 	| A String that defines the [Event](#define-your-events) |
| to		| &lt;state&gt;&nbsp;or&nbsp;&ast;	| The "to" State. If left blank or the state is "&ast;", then there is no change from the current state |

When a Transition is invoked, the *StatefulJ Framework* will invoke the associated method.  The first two parameters are always:

1. Stateful Entity
2. The Event

When the Stateful Framework binds the Endpoint, it will read all the Annotations on the method and all the Parameters after the StatefulEntity and Event and propagate to the Endpoint.  So, can define your  Transition with the parameters and annotations would normally would for the Endpoint. 

**Note:** If your method returns a String, and that String is prefixed with **event:**, then the return value will be treated as Event and re-propagated.  

```java
@StatefulController(
	clazz=Foo.class,
	startState=NON_EXISTENT,
	noops={
		@Transition(from=UPGRADE_PENDING, event=UPGRADE_APPROVED, to=UPGRADED)
	}
)
public class FooController {

	// Events
	//
	public static final String CREATE_FOO = "springmvc:post:/foo";
	public static final String GET_FOO = "springmvc:get:/foo/{id}";
	public static final String UPGRADE_REQUEST = 
								"springmvc:post:/foo/{id}/upgrade";
	public static final String UPGRADE_APPROVED = "upgrade.approved";

	@Transitions({
		@Transition(from=NON_EXISTENT, event=CREATE_FOO, to=ACTIVE),
		@Transition(from=ACTIVE, event=UPGRADE_REQUEST, to=UPGRADE_PENDING),
		@Transition(event=GET_FOO)
	})
	public String details(Foo foo, String event, Model model) {
		model.addAttribute("foo", foo);
		return "foo-details";
	}
}
```

### <a name="inject-stateful-fsm"></a> Inject the StatefulFSM

The *StatefulJ Framework* provides the ability to route *Events* directly to the *FSM*.  This is done by invoking the **onEvent** method of the associated *StatefulFSM* class.  To obtain a reference to the StatefulFSM, you annotate a reference with the [@FSM](/public/javadoc/org/statefulj/framework/core/annotations/FSM.html) annotation.  The StatefulJ Framewok will automatically inject the StatefulFSM based off the declared Generic Type.  If there is more than one [StatefulController](#define-your-controller) that qualifies, then you can provide the bean Id of the specific StatefulContoller.

| Field	   	| Description		|
|----		|----				|
| value		| The Id of the Stateful Controller.  If not specified, it will determine the FSM based off the Type |

```java

@FSM
StatefulFSM<Foo> fsm;

public void upgradeApproved(Foo foo) {
	fsm.onEvent(foo, UPGRADE_APPROVED);
}
```
