/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package it.analysis;

import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.build.SonarRunnerInstaller;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import it.Category3Suite;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
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
import org.sonar.wsclient.issue.IssueClient;
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
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

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
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample", null, "sonar.verbose", "true");
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
  public void project_key_with_slash() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    setDefaultQualityProfile("xoo", "one-issue-per-line");

    SonarRunner runner = configureRunner("shared/xoo-sample");
    runner.setProjectKey("sample/project");
    runner.setProperty("sonar.analysis.mode", "issues");
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);
  }

  // SONAR-6931
  @Test
  public void only_scan_changed_files_qps() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarRunner runner = configureRunner("shared/xoo-sample", "sonar.verbose", "true");
    BuildResult result = orchestrator.executeBuild(runner);
    List<Issue> serverIssues = ItUtils.getAllServerIssues(orchestrator);
    for (Issue i : serverIssues) {
      assertThat(i.status()).isEqualTo("OPEN");
    }
    assertThat(serverIssues).hasSize(17);

    // change quality profile
    restoreProfile("with-many-rules.xml");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");

    // do it again, scanning nothing (all files should be unchanged)
    runner = configureRunnerIssues("shared/xoo-sample", null,
      "sonar.verbose", "true");
    result = orchestrator.executeBuild(runner);
    assertThat(result.getLogs()).contains("Scanning only changed files");
    assertThat(result.getLogs()).contains("'One Issue Per Line' skipped because there is no related file in current project");
    ItUtils.assertIssuesInJsonReport(result, 0, 0, 17);
  }

  // SONAR-6931
  @Test
  public void only_scan_changed_files_transitions() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarRunner runner = configureRunner("shared/xoo-sample", "sonar.verbose", "true");
    BuildResult result = orchestrator.executeBuild(runner);
    List<Issue> serverIssues = ItUtils.getAllServerIssues(orchestrator);
    for (Issue i : serverIssues) {
      assertThat(i.status()).isEqualTo("OPEN");
    }
    assertThat(serverIssues).hasSize(17);

    // resolve 2 issues
    IssueClient issueClient = orchestrator.getServer().wsClient("admin", "admin").issueClient();
    issueClient.doTransition(serverIssues.get(0).key(), "wontfix");
    issueClient.doTransition(serverIssues.get(1).key(), "wontfix");

    // do it again, scanning nothing (all files should be unchanged)
    runner = configureRunnerIssues("shared/xoo-sample", null,
      "sonar.verbose", "true");
    result = orchestrator.executeBuild(runner);
    assertThat(result.getLogs()).contains("Scanning only changed files");
    assertThat(result.getLogs()).contains("'One Issue Per Line' skipped because there is no related file in current project");
    ItUtils.assertIssuesInJsonReport(result, 0, 0, 15);
  }

  // SONAR-6931
  @Test
  public void only_scan_changed_files_on_change() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarRunner runner = configureRunner("shared/xoo-sample", "sonar.verbose", "true");
    BuildResult result = orchestrator.executeBuild(runner);

    // change QP
    restoreProfile("with-many-rules.xml");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");

    // now change file hash in a temporary location
    File tmpProjectDir = temp.newFolder();
    FileUtils.copyDirectory(ItUtils.projectDir("shared/xoo-sample"), tmpProjectDir);
    File srcFile = new File(tmpProjectDir, "src/main/xoo/sample/Sample.xoo");
    FileUtils.write(srcFile, "\n", StandardCharsets.UTF_8, true);

    // scan again, with different QP
    runner = SonarRunner.create(tmpProjectDir,
      "sonar.working.directory", temp.newFolder().getAbsolutePath(),
      "sonar.analysis.mode", "issues",
      "sonar.report.export.path", "sonar-report.json",
      "sonar.userHome", temp.newFolder().getAbsolutePath(),
      "sonar.verbose", "true",
      "sonar.scanChangedFilesOnly", "true");
    result = orchestrator.executeBuild(runner);
    assertThat(result.getLogs()).contains("Scanning only changed files");
    assertThat(result.getLogs()).doesNotContain("'One Issue Per Line' skipped because there is no related file in current project");
    ItUtils.assertIssuesInJsonReport(result, 3, 0, 17);
  }

  @Test
  public void non_associated_mode() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    setDefaultQualityProfile("xoo", "one-issue-per-line");
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample-non-associated", null);
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

    SonarRunner runner = configureRunner("analysis/xoo-sample-with-spaces/v2");
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(getResource("sample:my sources/main/xoo/sample/My Sample.xoo")).isNotNull();

    runner = configureRunnerIssues("analysis/xoo-sample-with-spaces/v2", null);
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
    runner = configureRunnerIssues("shared/xoo-history-v2", null);
    result = orchestrator.executeBuild(runner);

    assertThat(getResource("sample:src/main/xoo/sample/ClassAdded.xoo")).isNull();
    assertThat(result.getLogs()).contains("Issues");
    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_fail_if_plugin_access_secured_properties() throws IOException {
    // Test access from task (ie BatchSettings)
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample", null,
      "accessSecuredFromTask", "true");
    BuildResult result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getLogs()).contains("Access to the secured property 'foo.bar.secured' is not possible in issues mode. "
      + "The SonarQube plugin which requires this property must be deactivated in issues mode.");

    // Test access from sensor (ie ModuleSettings)
    runner = configureRunnerIssues("shared/xoo-sample", null,
      "accessSecuredFromSensor", "true");
    result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getLogs()).contains("Access to the secured property 'foo.bar.secured' is not possible in issues mode. "
      + "The SonarQube plugin which requires this property must be deactivated in issues mode.");
  }

  // SONAR-4602
  @Test
  public void no_issues_mode_cache_by_default() throws Exception {
    File homeDir = runFirstAnalysisAndFlagIssueAsWontFix();

    // Second issues mode using same cache dir but cache disabled by default
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample", homeDir);
    BuildResult result = orchestrator.executeBuild(runner);

    // False positive is not returned
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(16);
  }

  // SONAR-7100
  @Test
  public void enable_issues_cache() throws Exception {
    File homeDir = runFirstAnalysisAndFlagIssueAsWontFix();

    // Second issues mode using same cache dir and enable cache
    SonarRunner runner = configureRunnerIssues("shared/xoo-sample", homeDir, "sonar.useWsCache", "true");
    BuildResult result = orchestrator.executeBuild(runner);

    // False positive is still visible since we are using cached issues
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(17);
  }

  private File runFirstAnalysisAndFlagIssueAsWontFix() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    // First run (publish mode)
    SonarRunner runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    // First issues mode
    File homeDir = temp.newFolder();
    runner = configureRunnerIssues("shared/xoo-sample", homeDir);
    BuildResult result = orchestrator.executeBuild(runner);

    // 17 issues
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(17);

    // Flag one issue as false positive
    JSONObject obj = ItUtils.getJSONReport(result);
    String key = ((JSONObject) ((JSONArray) obj.get("issues")).get(0)).get("key").toString();
    orchestrator.getServer().adminWsClient().issueClient().doTransition(key, "falsepositive");
    return homeDir;
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
    runner = configureRunnerIssues("shared/xoo-sample", null);
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

  private void runConcurrentIssues() throws Exception {
    // Install sonar-runner in advance to avoid concurrent unzip issues
    FileSystem fileSystem = orchestrator.getConfiguration().fileSystem();
    new SonarRunnerInstaller(fileSystem).install(Version.create(SonarRunner.DEFAULT_RUNNER_VERSION), fileSystem.workspace());
    final int nThreads = 3;
    ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
    List<Callable<BuildResult>> tasks = new ArrayList<>();
    final File homeDir = temp.newFolder();
    for (int i = 0; i < nThreads; i++) {
      tasks.add(new Callable<BuildResult>() {

        public BuildResult call() throws Exception {
          SonarRunner runner = configureRunnerIssues("shared/xoo-sample", homeDir, "sonar.it.enableWaitingSensor", "true");
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
        } else {
          throw e;
        }
      }
    }
    if (!expectedError) {
      fail("At least one of the threads should have failed");
    }
  }

  private void restoreProfile(String fileName) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/analysis/IssuesModeTest/" + fileName));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines"));
  }

  private SonarRunner configureRunnerIssues(String projectDir, @Nullable File homeDir, String... props) throws IOException {
    SonarRunner runner = SonarRunner.create(ItUtils.projectDir(projectDir),
      "sonar.working.directory", temp.newFolder().getAbsolutePath(),
      "sonar.analysis.mode", "issues",
      "sonar.report.export.path", "sonar-report.json");
    if (homeDir != null) {
      runner.setProperty("sonar.userHome", homeDir.getAbsolutePath());
    } else {
      runner.setProperty("sonar.userHome", temp.newFolder().getAbsolutePath());
    }
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
