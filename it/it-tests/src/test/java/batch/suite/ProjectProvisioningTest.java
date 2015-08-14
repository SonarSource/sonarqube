/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch.suite;

import static org.assertj.core.api.Assertions.assertThat;

import util.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.project.NewProject;
import org.sonar.wsclient.services.PropertyUpdateQuery;

public class ProjectProvisioningTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.resetData();
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
      );
  }

  @AfterClass
  public static void resetAutoProjectCreation() {
    setProperty("sonar.preventAutoProjectCreation", "false");
  }

  private SonarClient client;

  @Before
  public void initClient() {
    client = orchestrator.getServer().adminWsClient();
  }

  /**
   * SONAR-3871
   * SONAR-4713
   */
  @Test
  public void should_allow_existing_project_scan() {
    setProperty("sonar.preventAutoProjectCreation", "true");

    // xoo-sample already exists => pass
    checkBuildSuccess("shared/xoo-sample");
  }

  /**
   * SONAR-3871
   * SONAR-4713
   */
  @Test
  @Ignore("This test should be moved to a Medium test of the Compute Engine")
  public void should_prevent_project_creation() {
    setProperty("sonar.preventAutoProjectCreation", "true");

    // xoo-sample-with-tests does not exist => fail
    checkBuildFailed("shared/xoo-sample-with-tests");

    // provision xoo-sample-with-tests and retry
    client.projectClient().create(
      NewProject.create()
        .key("sample-with-tests")
        .name("Sample With Tests"));
    checkBuildSuccess("shared/xoo-sample-with-tests");
  }

  /**
   * SONAR-3871
   * SONAR-4713
   */
  @Test
  public void should_allow_provisioned_project() {
    setProperty("sonar.preventAutoProjectCreation", "true");

    // provision xoo-multi-modules-sample before 1st scan and check build OK
    client.projectClient().create(
      NewProject.create()
        .key("com.sonarsource.it.samples:multi-modules-sample")
        .name("Xoo Multi Modules Sample"));
    checkBuildSuccess("shared/xoo-multi-modules-sample");
  }

  /**
   * SONAR-5547
   */
  @Test
  public void should_allow_provisioned_project_with_branch() {
    setProperty("sonar.preventAutoProjectCreation", "true");

    // provision xoo-multi-modules-sample before 1st scan and check build OK
    client.projectClient().create(
      NewProject.create()
        .key("com.sonarsource.it.samples:multi-modules-sample:branch")
        .name("Xoo Multi Modules Sample - Branch"));
    checkBuildSuccess("shared/xoo-multi-modules-sample", "sonar.branch", "branch");
  }

  /**
   * SONAR-3871
   * SONAR-4713
   */
  @Test
  public void should_allow_provisioned_project_even_when_provisioning_not_enforced() {
    setProperty("sonar.preventAutoProjectCreation", "false");

    client.projectClient().create(
      NewProject.create()
        .key("xo")
        .name("xo"));
    checkBuildSuccess("shared/xoo-two-letters-named");
  }

  private static BuildResult checkBuildSuccess(String projectPath, String... propertiesKeyValues) {
    BuildResult result = scan(projectPath, propertiesKeyValues);
    assertThat(result.getStatus()).isZero();
    return result;
  }

  private static BuildResult checkBuildFailed(String projectPath) {
    BuildResult result = scan(projectPath);
    assertThat(result.getStatus()).isNotEqualTo(0);
    return result;
  }

  private static BuildResult scan(String projectPath, String... propertiesKeyValues) {
    return orchestrator.executeBuildQuietly(
      SonarRunner.create(ItUtils.projectDir(projectPath)).setProperties(propertiesKeyValues));
  }

  private static void setProperty(String key, String value) {
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery(key, value));
  }
}
