# sonar-db-commons package

This package contains common database access utilities. It depends upon MyBatis and HikariCP,
and defines some patterns for using those.

## Basic components

These components are included:

* a DbSession class which wraps the MyBatis SqlSession
* a DbSessions class which can cache a DbSession per-thread when enabled
* MyBatis default configuration and a mechanism for multiple extensions to contribute to MyBatis configuration
* an AbstractDbClient which can be subclassed to create convenient `*DbClient` classes that bundle some related Dao and
  a DbSessions instance
* DatabaseUtils with some static utility methods
* a Dialect class to retrieve quirks for different db dialects
* a few other useful utilities
* some useful test fixtures

This package does NOT contain anything that relies on a particular database schema, nor does
it assume that it is running in the SonarQube Server process talking to the SonarQube Server
database. This package also does not introduce any migration framework.

To use the package, create concrete subclasses of the Database and MyBatis interfaces.

The main job of the concrete Database subclass is to figure out how to connect to
an actual database and obtain a jdbc DataSource. There might be multiple Database subclasses
if a process needs to talk to multiple databases.

The concrete MyBatis implementation configures MyBatis with the desired mappers and other settings,
and starts it up connected to a Database. The AbstractMyBatis base class simplifies implementation of
the MyBatis interface. There would typically be multiple MyBatis implementations if there are
multiple databases.

Inside SonarQube Server, there's only one Database and one MyBatis, so those can be injected without
further qualification. If an application has multiple databases, then injecting Database or
MyBatis might be ambiguous and result in a Spring error, so it would be necessary to disambiguate
using more specific classes or perhaps the Spring `@Qualifier` annotation.

## TestDb

TestDb is a dynamically-providable interface for creating a test database instance with a schema,
that can be used by "DbTester" classes (AbstractSqlDbTester, AbstractDbTester, AbstractPluggableDbTester).

TestDb allows tests to be agnostic about the exact database setup, so the same tests can run against
multiple database setups, verifying that those setups are equivalent from the perspective of your application code.

When we say "database setup" here, the intention is to allow testing against different migration
frameworks, schemas, and configuration mechanisms, for example.
To simply test against multiple different database dialects, SonarQube Server swaps out the
jdbc URL but uses the same TestDb.

## AbstractSqlDbTester, AbstractDbTester, AbstractPluggableDbTester

* AbstractSqlDbTester is a JUnit extension that manages a TestDb instance lifecycle and truncates table data between
  tests for isolation. It also provides a collection of SQL utility methods.
* AbstractDbTester extends AbstractSqlDbTester by adding a cached MyBatis session that can be used in tests.
* AbstractPluggableDbTester extends AbstractDbTester by using ServiceLoader to dynamically load a TestDb implementation
  at runtime.

Typical applications might subclass AbstractPluggableDbTester and pass in their MyBatis configuration extensions.
They would also select one or more suitable TestDb implementations by setting system properties
in gradle.

If you have a concrete `MyDbTester extends AbstractPluggableDbTester`, then tests would have something like this:

```
@RegisterExtension
MyDbTester dbTester = MyDbTester.create();
```

Within SonarQube Server, everything uses the concrete subclass DbTester, which is in the
SonarQube Server codebase and not sonar-db-commons.

## Gradle sample for running tests against multiple database setups

Here is an example Gradle configuration to run tests against both the SonarQube Server
database and an alternative cloud database. (If you can improve this configuration please
do!)  The purpose of this is to be sure your tests pass in just the same way against
both database setups.

```
// Test task for server database (H2 by default but configurable with orchestrator.configUrl)
tasks.register('testWithServerDb', Test) {
  description = 'Runs tests using server db framework'
  group = 'verification'

  testClassesDirs = sourceSets.test.output.classesDirs
  classpath = sourceSets.test.runtimeClasspath

  useJUnitPlatform()
  systemProperty 'org.sonar.db.TestDbProvider.provider',
    'org.sonar.db.ServerTestDbProvider'

  // without this, we don't use the correct database to test with in CI
  systemProperty 'orchestrator.configUrl', System.getProperty('orchestrator.configUrl')

  // Use different test results directory to avoid conflicts
  reports {
    html.outputLocation = file("${buildDir}/reports/tests/serverDb")
    junitXml.outputLocation = file("${buildDir}/test-results/serverDb")
  }
}

// Test task for cloud TestDb setup (PostgreSQL testcontainers)
tasks.register('testWithCloudDb', Test) {
  description = 'Runs tests using cloud db framework'
  group = 'verification'

  testClassesDirs = sourceSets.test.output.classesDirs
  classpath = sourceSets.test.runtimeClasspath

  useJUnitPlatform()
  systemProperty 'org.sonar.db.TestDbProvider.provider',
    'org.sonar.example.cloud.db.CloudTestDbProvider'

  // Use different test results directory to avoid conflicts
  reports {
    html.outputLocation = file("${buildDir}/reports/tests/cloudDb")
    junitXml.outputLocation = file("${buildDir}/test-results/cloudDb")
  }
}

// Convenience task to run all database variants
tasks.register('testWithCloudAndServerDbs') {
  description = 'Runs tests against both cloud and server database implementations'
  group = 'verification'
  dependsOn testWithServerDb, testWithCloudDb
}

// Make default test task depend on both variants
// This means `gradle test` and `gradle build` will run both test suites
test {
  dependsOn testWithCloudAndServerDbs
  // Disable the test task itself since we're delegating to the specific variants
  actions.clear()
}
```
