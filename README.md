# slick-akka-http
The Slick Akka Http is a very simple json rest api showing one way of using akka http with [slick 3](https://github.com/slick/slick) library for database access.


It supports the following features:

* Generic Data Access layer, create a DAL with crud for an entity with just one line
* Models as case classes and slick models, independent from database driver and profile
* Multiple database types configured in properties file (h2 and postgresql for instance)
* Cake pattern for DI
* Spray-json to parse json
* Tests for DAL
* tests for routes

Utils: 

* Typesafe config for property management
* Typesafe Scala Logging (LazyLogging)

The project was thought to be used as an activator template.

#Running

The database pre-configured is an h2, so you just have to:


        $ sbt run

#Testing

To run all tests (routes and persistence tests):


        $ sbt test


#TODO

Swagger

#Credits

To make this template, I just mixed the tutorials and templates, so credits for akka and slick guys.
