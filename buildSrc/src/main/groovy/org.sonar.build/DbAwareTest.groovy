package org.sonar.build

import java.time.Duration
import org.gradle.api.tasks.testing.Test

/**
 * Configures a Test task to connect to the live database exercised by the DB JUnit CI job
 * (Postgres/Oracle/MSSQL), instead of TestDbImpl silently falling back to H2, and to key its
 * build-cache entry per DB engine version so different versions of the same vendor (e.g.
 * Postgres 15 vs 18) don't share a cache hit.
 *
 * Apply to every module that ships its own SQL, and list it in that job's script
 * (.github/ci-files/run-db-unit-test.sh). DbVendorTestCoverageCheck enforces both sides.
 */
class DbAwareTest {
  static final String MARKER = 'sonarDbAwareTest'
  // Read by OrchestratorSettingsUtils in the sonar-db-migration test fixtures
  static final String DEDICATED_DATABASE_PROPERTY = 'sonar.test.dedicatedDatabase'
  static final List<String> JDBC_DRIVERS = [
    'com.microsoft.sqlserver:mssql-jdbc',
    'com.oracle.database.jdbc:ojdbc11',
    'org.postgresql:postgresql',
  ]

  static void configure(Test test) {
    test.systemProperty('orchestrator.configUrl', System.getProperty('orchestrator.configUrl'))
    test.inputs.property('dbImage', System.getProperty('sonar.test.dbImage')).optional(true)
    test.extensions.extraProperties.set(MARKER, true)
    if (isVendorRun()) {
      // A test blocked on a database lock never times out on its own, and a JUnit timeout can't interrupt a JDBC read.
      // Sized for the longest suite: all migration versions on Oracle in the nightly.
      test.timeout.set(Duration.ofMinutes(90))
      // Each test task creates and owns its database, so that test tasks can run in parallel
      test.systemProperty(DEDICATED_DATABASE_PROPERTY, dedicatedDatabaseName(test))
      test.maxParallelForks = 1
    }
    JDBC_DRIVERS.each { test.project.dependencies.add('testRuntimeOnly', it) }
  }

  /**
   * Like {@link #configure(Test)}, but on vendor databases only runs the test classes matching the given patterns,
   * for modules where a few classes cover the module's own SQL. All tests still run on H2.
   */
  static void configure(Test test, List<String> vendorTestPatterns) {
    configure(test)
    if (isVendorRun()) {
      test.filter.failOnNoMatchingTests = true
      vendorTestPatterns.each { test.filter.includeTestsMatching(it) }
    }
  }

  static boolean isVendorRun() {
    System.getProperty('sonar.test.dbImage') != null
  }

  /** Also the login, so it must be a valid unquoted identifier on every vendor, at most 63 characters (PostgreSQL). */
  static String dedicatedDatabaseName(Test test) {
    String name = "${test.project.path.substring(1)}_${test.name}".replaceAll(/[^A-Za-z0-9]/, '_').toLowerCase()
    if (name.length() > 63) {
      throw new IllegalStateException("Dedicated database name '$name' of ${test.path} exceeds 63 characters")
    }
    name
  }

  static boolean isConfigured(Test test) {
    test.extensions.extraProperties.has(MARKER)
  }
}
