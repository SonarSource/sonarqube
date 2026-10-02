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

  static boolean isConfigured(Test test) {
    test.extensions.extraProperties.has(MARKER)
  }
}
