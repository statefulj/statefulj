---
layout: page
title: StatefulJ Persistence
weight: 0
---

StatefulJ provides different *Persisters* which persists State changes in an atomic, thread-safe manner.  The Persisters are invoked by the [*StatefulJ FSM*](/fsm/) library on every [*State Transition*](/fsm/#define-your-transitions) prior to the invocation of the associated [*Action*](/fsm#define-your-actions).  If the Persister is unable to update the State due to staleness - it will reload a new copy of the State and retry the event.

StatefulJ provides the following Persisters:

### <a name="in-memory-persister"></a>In-Memory Persister
	
By default, the [*StatefulJ FSM*](/fsm/) library provides an in-memory Persister.  This Persister updates the State field of the Entity only in the in-process memory.  The updates are thread-safe; however, multiple process in different application spaces will not be aware of the State changes.  Additionally, if the Entities are then being persisted to the database - this will cause problems as updates are now non-atomic.
	
If you need to support multiple processes or are persisting the Entities to a database, you will need to choose one of the database Persisters

### <a name="jpa-persister"></a>JPA Persister

The JPA Persister will atomically update the State of the Entity in a SQL Database.  The JPA Persister is dependent upon a correctly configured [Entity Manager](http://docs.oracle.com/javaee/7/api/javax/persistence/EntityManager.html) and will automatically assume the scope of an existing Transaction (it doesn't start a new Transaction).
	
#### Maven
```xml
	<dependency>
		<groupId>org.statefulj.persistence</groupId>
		<artifactId>statefulj-persistence-jpa</artifactId>
		<version>{{ site.version }}</version>
	</dependency>	
```

**Setup**


**Note**

If you are utilizing the [*StatefulJ Framework Persisters*](/framework#installation-persisters/), then you will not need to explicitly include the JPA library - it's included as a dependency of the Framework.

### <a name="mongo-persister"></a>Mongo Persister

The Mongo Persister will atomically update the State of the Entity in a [Mongo Database](http://www.mongodb.org/).  The Mongo Persister is dependent upon a [Spring Data MongoDB](http://projects.spring.io/spring-data-mongodb/).  The Persiter requires that Entity have a [MongoRepository](http://docs.spring.io/spring-data/data-mongo/docs/current/api/org/springframework/data/mongodb/repository/MongoRepository.html) defined for the Entity.  The MongoRepository.
	
#### Maven
```xml
	<dependency>
		<groupId>org.statefulj.persistence</groupId>
		<artifactId>statefulj-persistence-mongo</artifactId>
		<version>{{ site.version }}</version>
	</dependency>	
```

**Note**

If you are utilizing the [*StatefulJ Framework Persisters*](/framework#installation-persisters/), then you will not need to explicitly include the Mongo library - it's included as a dependency of the Framework.