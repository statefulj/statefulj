---
layout: page
title: Introduction
weight: 0
---

## What is StatefulJ?

[StatefulJ](http://www.statefulj.org) is an open source Java based Finite State Machine along with a Spring based integrated framework.


## Why should I use StatefulJ in my Application?

Modern day applications have to manage and orchestrate requests and events from many sources: REST and SOAP APIs, message queueing systems, internal events, page requests, etc...

Handling concurrency and reliably updating Application state becomes a major challenge without the proper framework.  StatefulJ integrates State Machines into your Application providing you the ability to easily define and manage your state models.

## What does StatefulJ provide?

StatefulJ provides the following "packages":

* **StatefulJ Core:** A dependency free, Finite State Machine implementation with support for non-determinstic Transitions.
* **StatefulJ Persistence:** A set of persistence support (JPA and Mongo) which works with the Core library to persist your Stateful objects.
* **StatefulJ Framework:** A framework built off StatefulJ Core, StatefulJ Persistence and Spring Data to easily integrate State Machines into your Application.

## Options

Hyde includes some customizable options, typically applied via classes on the `<body>` element.


### Sidebar menu

Create a list of nav links in the sidebar by assigning each Jekyll page the correct layout in the page's [front-matter](http://jekyllrb.com/docs/frontmatter/).


**Mark Otto**
- <https://github.com/mdo>
- <https://twitter.com/mdo>

```html
<body class="theme-base-08">
  ...
</body>
```

```java
public class Foo {
	
	public void Bar() {
		String foo = "bar";
	}
	
}
```