---
layout: page
title: Demos
weight: 0
---

## Demos

### Mongo Persistence Demo

This demonstrates how to construct a FSM with the Mongo Persister.

#### Steps

1. `git clone git@github.com:statefulj/statefulj-persistence-mongo-demo.git`
1. `cd statefulj-persistence-mongo-demo`
1. `mvn spring-boot:run`

### JPA Based Banking Demo

A complete Banking Application running on an embedded [HyperSQL Database](http://hsqldb.org/), [Jetty Server](http://www.eclipse.org/jetty/), [SpringMVC UI](http://projects.spring.io/spring-framework/), [Camel Messaging](http://camel.apache.org/) and [Jersey based JAX-RS](https://jersey.java.net/).

#### Steps

1. `git clone git@github.com:statefulj/statefulj-framework-demo-jpa.git`
1. `cd statefulj-framework-demo-jpa`
1. `mvn jetty:run`
1. `Open browser to http://localhost:8080`

### Mongo Based Banking Demo

A complete Banking Application running on an embedded [Mongo](http://www.mongodb.org/), [Jetty Server](http://www.eclipse.org/jetty/), [SpringMVC UI](http://projects.spring.io/spring-framework/), [Camel Messaging](http://camel.apache.org/) and [Jersey based JAX-RS](https://jersey.java.net/).

#### Steps

1. `git clone git@github.com:statefulj/statefulj-framework-demo-mongo.git`
1. `cd statefulj-framework-demo-mongo`
1. `mvn jetty:run`
1. `Open browser to http://localhost:8080`