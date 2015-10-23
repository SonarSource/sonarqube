/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import it.Category3Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProjectExclusionsTest {
  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Before
  public void deleteProjectData() {
    orchestrator.resetData();
  }

  /**
   * This use-case was a bug in 2.8-RC2. It failed when both the properties sonar.branch and sonar.skippedModules
   * were set on the same multi-modules project.
   */
  @Test
  public void shouldSupportMixOfBranchAndSkippedModules() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("shared/multi-modules-sample"))
      .setGoals("clean verify", "sonar:sonar")
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.branch", "mybranch")
      .setProperty("sonar.skippedModules", "module_b");

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample:mybranch"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a:mybranch").getId());
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1:mybranch").getId());
    assertNotNull(getResource("com.sonarsource.it.samples:module_a2:mybranch").getId());

    assertNull(getResource("com.sonarsource.it.samples:module_b:mybranch"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1:mybranch"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2:mybranch"));
  }

  /**
   * Black list
   */
  @Test
  public void shouldExcludeModuleAndItsChildren() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("shared/multi-modules-sample"))
      .setGoals("clean verify", "sonar:sonar")
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.skippedModules", "module_b");

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a2"));

    // excluded project and its children
    assertNull(getResource("com.sonarsource.it.samples:module_b"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2"));
  }

  /**
   * Exhaustive white list
   */
  @Test
  public void shouldIncludeModules() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("shared/multi-modules-sample"))
      .setGoals("clean verify", "sonar:sonar")
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.includedModules", "multi-modules-sample,module_a,module_a1");

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1"));

    assertNull(getResource("com.sonarsource.it.samples:module_a2"));
    assertNull(getResource("com.sonarsource.it.samples:module_b"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2"));
  }

  @Test
  public void rootModuleShouldBeOptionalInListOfIncludedModules() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("shared/multi-modules-sample"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      // the root module 'multi-modules-sample' is not declared
      .setProperty("sonar.includedModules", "module_a,module_a1");

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1"));

    assertNull(getResource("com.sonarsource.it.samples:module_a2"));
    assertNull(getResource("com.sonarsource.it.samples:module_b"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2"));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.create(key));
  }
}
