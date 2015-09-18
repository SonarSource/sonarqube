/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch;

import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.build.SonarRunnerInstaller;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang.ObjectUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.user.UserParameters;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class IssuesModeTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion("DEV")
    .addPlugin(ItUtils.xooPlugin())
    .setContext("/")

  .addPlugin(ItUtils.pluginArtifact("access-secured-props-plugin"))
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() throws IOException {
    orchestrator.resetData();
  }

  @Test
  public void issues_analysis_on_new_project() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample", "sonar.verbose", "true");
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);
  }

  @Test
  public void invalid_incremental_mode() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarRunner runner = configureRunner("shared/xoo-sample");
    runner.setProperty("sonar.analysis.mode", "incremental");

    thrown.expect(BuildFailureException.class);
    BuildResult res = orchestrator.executeBuild(runner);

    assertThat(res.getLogs()).contains("Invalid analysis mode: incremental. This mode was removed in SonarQube 5.2");
  }

  @Test
  public void non_associated_mode() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    setDefaultQualityProfile("xoo", "one-issue-per-line");
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample-non-associated");
    BuildResult result = orchestrator.executeBuild(runner);

    assertThat(result.getLogs()).contains("Local analysis");
    assertThat(result.getLogs()).contains("Cache not found, synchronizing data");
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);

    result = orchestrator.executeBuild(runner);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);
    assertThat(result.getLogs()).contains("Found cache");
  }

  // SONAR-5715
  @Test
  public void test_issues_mode_on_project_with_space_in_filename() throws IOException {
    restoreProfile("with-many-rules.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample-with-spaces");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");

    SonarRunner runner = configureRunner("batch/xoo-sample-with-spaces/v2");
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(getResource("sample:my sources/main/xoo/sample/My Sample.xoo")).isNotNull();

    runner = configureRunnerIssues("batch/xoo-sample-with-spaces/v2");
    result = orchestrator.executeBuild(runner);
    // Analysis is not persisted in database
    Resource project = getResource("com.sonarsource.it.samples:simple-sample");
    assertThat(project).isNull();
    assertThat(result.getLogs()).contains("Issues");
    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_not_fail_on_resources_that_have_existed_before() throws IOException {
    restoreProfile("with-many-rules.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-history");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");

    // First real scan with source
    SonarRunner runner = configureRunner("shared/xoo-history-v2");
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(getResource("sample:src/main/xoo/sample/ClassAdded.xoo")).isNotNull();

    // Second scan should remove ClassAdded.xoo
    runner = configureRunner("shared/xoo-history-v1");
    result = orchestrator.executeBuild(runner);
    assertThat(getResource("sample:src/main/xoo/sample/ClassAdded.xoo")).isNull();

    // Re-add ClassAdded.xoo in local workspace
    runner = configureRunnerIssues("shared/xoo-history-v2");
    result = orchestrator.executeBuild(runner);

    assertThat(getResource("sample:src/main/xoo/sample/ClassAdded.xoo")).isNull();
    assertThat(result.getLogs()).contains("Issues");
    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_fail_if_plugin_access_secured_properties() throws IOException {
    // Test access from task (ie BatchSettings)
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample",
      "accessSecuredFromTask", "true");
    BuildResult result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getLogs()).contains("Access to the secured property 'foo.bar.secured' is not possible in issues mode. "
      + "The SonarQube plugin which requires this property must be deactivated in issues mode.");

    // Test access from sensor (ie ModuleSettings)
    runner = configureRunnerIssues("shared/xoo-sample",
      "accessSecuredFromSensor", "true");
    result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getLogs()).contains("Access to the secured property 'foo.bar.secured' is not possible in issues mode. "
      + "The SonarQube plugin which requires this property must be deactivated in issues mode.");
  }

  // SONAR-4602
  @Test
  public void no_issues_mode_cache_after_new_analysis() throws Exception {
    restoreProfile("one-issue-per-line.xml");
    restoreProfile("empty.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");

    // First run (publish mode)
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "empty");
    SonarRunner runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    // First run issues mode
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    runner = configureRunnerIssues("shared/xoo-sample");
    BuildResult result = orchestrator.executeBuild(runner);

    // As many new issue as lines
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);

    // Second run (publish mode) should invalidate cache
    runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    // Second run issues mode
    runner = configureRunnerIssues("shared/xoo-sample", "sonar.report.export.path", "sonar-report.json");
    result = orchestrator.executeBuild(runner);

    // No new issue this time
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(0);
  }

  // SONAR-4602
  @Test
  public void no_issues_mode_cache_after_profile_change() throws Exception {
    restoreProfile("one-issue-per-line-empty.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    // First run (publish mode)
    SonarRunner runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    // First issues mode
    runner = configureRunnerIssues("shared/xoo-sample");
    BuildResult result = orchestrator.executeBuild(runner);

    // No new issues
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(0);

    // Modification of QP should invalidate cache
    restoreProfile("/one-issue-per-line.xml");

    // Second issues mode
    runner = configureRunnerIssues("shared/xoo-sample", "sonar.report.export.path", "sonar-report.json");
    result = orchestrator.executeBuild(runner);

    // As many new issue as lines
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);
  }

  // SONAR-4602
  @Test
  public void no_issues_mode_cache_after_issue_change() throws Exception {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    // First run (publish mode)
    SonarRunner runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    // First issues mode
    runner = configureRunnerIssues("shared/xoo-sample");
    BuildResult result = orchestrator.executeBuild(runner);

    // 17 issues
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(17);

    // Flag one issue as false positive
    JSONObject obj = ItUtils.getJSONReport(result);
    String key = ((JSONObject) ((JSONArray) obj.get("issues")).get(0)).get("key").toString();
    orchestrator.getServer().adminWsClient().issueClient().doTransition(key, "falsepositive");

    // Second issues mode
    runner = configureRunnerIssues("shared/xoo-sample");
    result = orchestrator.executeBuild(runner);

    // False positive is not returned
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(16);
  }

  // SONAR-6522
  @Test
  public void load_user_name_in_json_report() throws Exception {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    // First run (publish mode)
    SonarRunner runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    SonarClient client = orchestrator.getServer().adminWsClient();

    Issues issues = client.issueClient().find(IssueQuery.create());
    Issue issue = issues.list().get(0);

    UserParameters creationParameters = UserParameters.create().login("julien").name("Julien H")
      .password("password").passwordConfirmation("password");
    client.userClient().create(creationParameters);

    // Assign issue
    client.issueClient().assign(issue.key(), "julien");

    // Issues
    runner = configureRunnerIssues("shared/xoo-sample");
    BuildResult result = orchestrator.executeBuild(runner);

    JSONObject obj = ItUtils.getJSONReport(result);

    Map<String, String> userNameByLogin = Maps.newHashMap();
    final JSONArray users = (JSONArray) obj.get("users");
    if (users != null) {
      for (Object user : users) {
        String login = ObjectUtils.toString(((JSONObject) user).get("login"));
        String name = ObjectUtils.toString(((JSONObject) user).get("name"));
        userNameByLogin.put(login, name);
      }
    }
    assertThat(userNameByLogin.get("julien")).isEqualTo("Julien H");

    for (Object issueJson : (JSONArray) obj.get("issues")) {
      JSONObject jsonObject = (JSONObject) issueJson;
      if (issue.key().equals(jsonObject.get("key"))) {
        assertThat(jsonObject.get("assignee")).isEqualTo("julien");
        return;
      }
    }
    fail("Issue not found");
  }

  @Test
  public void concurrent_issue_mode_on_existing_project() throws Exception {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarRunner runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    runConcurrentIssues();
  }

  private void runConcurrentIssues() throws InterruptedException, ExecutionException {
    // Install sonar-runner in advance to avoid concurrent unzip issues
    FileSystem fileSystem = orchestrator.getConfiguration().fileSystem();
    new SonarRunnerInstaller(fileSystem).install(Version.create(SonarRunner.DEFAULT_RUNNER_VERSION), fileSystem.workspace());
    final int nThreads = 3;
    ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
    List<Callable<BuildResult>> tasks = new ArrayList<>();
    for (int i = 0; i < nThreads; i++) {
      tasks.add(new Callable<BuildResult>() {

        public BuildResult call() throws Exception {
          SonarRunner runner = configureRunnerIssues("shared/xoo-sample");
          return orchestrator.executeBuild(runner);
        }
      });
    }

    boolean expectedError = false;
    for (Future<BuildResult> result : executorService.invokeAll(tasks)) {
      try {
        result.get();
      } catch (ExecutionException e) {
        if (e.getCause() instanceof BuildFailureException) {
          BuildFailureException bfe = (BuildFailureException) e.getCause();
          assertThat(bfe.getResult().getLogs()).contains("Another SonarQube analysis is already in progress for this project");
          expectedError = true;
        }
      }
    }
    if (!expectedError) {
      fail("At least one of the threads should have failed");
    }
  }

  private void restoreProfile(String fileName) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/batch/IssuesModeTest/" + fileName));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines"));
  }

  private SonarRunner configureRunnerIssues(String projectDir, String... props) throws IOException {
    SonarRunner runner = SonarRunner.create(ItUtils.projectDir(projectDir),
      "sonar.working.directory", temp.newFolder().getAbsolutePath(),
      "sonar.analysis.mode", "issues",
      "sonar.report.export.path", "sonar-report.json",
      "sonar.userHome", temp.newFolder().getAbsolutePath());
    runner.setProperties(props);
    return runner;
  }

  private SonarRunner configureRunner(String projectDir, String... props) throws IOException {
    SonarRunner runner = SonarRunner.create(ItUtils.projectDir(projectDir),
      "sonar.working.directory", temp.newFolder().getAbsolutePath(),
      "sonar.report.export.path", "sonar-report.json",
      "sonar.userHome", temp.newFolder().getAbsolutePath());
    runner.setProperties(props);
    return runner;
  }

  private void setDefaultQualityProfile(String languageKey, String profileName) {
    orchestrator.getServer().adminWsClient().post("api/qualityprofiles/set_default",
      "language", languageKey,
      "profileName", profileName);
  }

}
