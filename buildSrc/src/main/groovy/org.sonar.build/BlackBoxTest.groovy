package org.sonar.build

import org.gradle.api.tasks.testing.Test

class BlackBoxTest extends Test {
  BlackBoxTest() {
    def branch = System.getenv('GITHUB_BRANCH')
    if (branch != null && Set.of("branch-nightly-build", "master").contains(branch)) {
      jvmArgs("-javaagent:" + System.getenv('ASPECTJ_WEAVER_PATH'))
    }

    systemProperty 'java.awt.headless', 'true'
    systemProperty 'orchestrator.configUrl', System.getProperty('orchestrator.configUrl')
    systemProperty 'webdriver.chrome.driver', System.getProperty('webdriver.chrome.driver')

    if (!project.version.endsWith("-SNAPSHOT")) {
      systemProperty 'sonar.runtimeVersion', project.version
    }

    testLogging {
      events "skipped", "failed"
      showStandardStreams = true
      exceptionFormat 'full'
    }
  }
}
