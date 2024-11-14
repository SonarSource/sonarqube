package org.sonar.build

import org.gradle.api.tasks.testing.Test

abstract class BlackBoxTest extends Test {
  BlackBoxTest() {
    systemProperty 'java.awt.headless', 'true'
    systemProperty 'orchestrator.configUrl', System.getProperty('orchestrator.configUrl')
    systemProperty 'webdriver.chrome.driver', System.getProperty('webdriver.chrome.driver')

    if (!project.version.endsWith("-SNAPSHOT")) {
      systemProperty 'sonar.runtimeVersion', project.version
      systemProperty 'sonar.communityRuntimeVersion', project.communityVersion
    }

    testLogging {
      events "skipped", "failed"
      showStandardStreams = true
      exceptionFormat 'full'
    }
  }
}
