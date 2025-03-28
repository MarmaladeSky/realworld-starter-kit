# ![RealWorld Example App](logo.png)

# RealWorld Http4s + Cats + Slick

This example of the [RealWorld](https://github.com/gothinkster/realworld) spec and API implementation is based on not
widely used combination of Typelevel and Slick. The goal is to achieve more type safety in DB actions and make changes
of DB schema easier.

# Stack

- [Cats](https://github.com/typelevel/cats)
- [Http4s](https://github.com/http4s/http4s)
- [Slick](https://github.com/slick/slick)
- [PostgreSQL](https://www.postgresql.org/)
- [Flyway](https://github.com/flyway/flyway)

# The use case

The main use case of this composition style is the support of complex CRUD+L APIs that could get changes in DB schema.
We make these changes "driven by DB schema" and perform them with steps below:

- add a Flyway change
- re-generate Slick Models
- fix compilation errors and make required changes in the codebase

# How it works

### Code composition

```
    +----------------+
    |     Routes     |
    +----------------+
             |
             v
    +----------------+
    |     Service    |
    +----------------+
             |
             v
  +--------------------+
  |     Repository     |
  +--------------------+
```

### Packages

`routes` package defines APIs and exposed models

`services` package defines business rules and internal models

`db` package defines persistence and DB models

### Getting started

```bash
# Run DB instance, for example, with docker
docker run -p 5432:5432 --name some-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=condoit -d postgres

# Init schema with flyway (assume your working dir is the cloned repo)
flyway \flyway \
  -url="jdbc:postgresql://localhost:5432/condoit" \
  -user="postgres" \
  -password="postgres" \
  -createSchemas="true" \
  -schemas="condoit" \
  -locations="filesystem:./sql/" \
  -X migrate
  
# Run server
# The configuration is made with .env file, default values are provided
sbt run
```

### Making changes

To perform changes, you need an initialized DB from the previous step above.

```bash
echo "ALTER TABLE users ADD COLUMN country TEXT NOT NULL;" > ./sql/V2__changeset.sql

# Perform schema changes at the running DB
flyway \
  -url="jdbc:postgresql://localhost:5432/condoit" \
  -user="postgres" \
  -password="postgres" \
  -createSchemas="true" \
  -schemas="condoit" \
  -locations="filesystem:./sql/" \
  -X migrate -outputType=json

# Regenerate Slick Schema
sbt clean compile 'slickPgGen localhost 5432 condoit condoit postgres postgres'

# Fix compilation errors and add changes at business logic, routes and models
```

### Tools
Here's the list of tools I find useful in day to day work.

### SBT Explicit Dependencies plugin
The plugin to avoid dependency hell. A nice tool to have in your CI/CD pipeline.
```shell
sbt 'undeclaredCompileDependenciesTest; unusedCompileDependenciesTest'
```
~~~~