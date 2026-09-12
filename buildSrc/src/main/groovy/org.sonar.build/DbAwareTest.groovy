package org.sonar.build

import org.gradle.api.tasks.testing.Test

/**
 * Configures a Test task to connect to the live database exercised by the DB JUnit CI job
 * (Postgres/Oracle/MSSQL), instead of TestDbImpl silently falling back to H2, and to key its
 * build-cache entry per DB engine version so different versions of the same vendor (e.g.
 * Postgres 15 vs 18) don't share a cache hit.
 *
 * Apply to every module whose tests use DbTester/TestDb and is invoked by that job's script
 * (.github/ci-files/run-db-unit-test.sh).
 */
class DbAwareTest {
  static void configure(Test test) {
    test.systemProperty('orchestrator.configUrl', System.getProperty('orchestrator.configUrl'))
    test.inputs.property('dbImage', System.getProperty('sonar.test.dbImage')).optional(true)
  }
}
