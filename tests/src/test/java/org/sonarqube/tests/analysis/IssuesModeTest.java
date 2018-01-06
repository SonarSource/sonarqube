/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.tests.analysis;

import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.build.SonarScannerInstaller;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.version.Version;
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
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.ws.client.PostRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.getComponent;

public class IssuesModeTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void issues_analysis_on_new_project() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarScanner runner = configureScannerIssues("shared/xoo-sample", null, "sonar.verbose", "true");
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);
  }

  @Test
  public void invalid_incremental_mode() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarScanner scanner = configureScanner("shared/xoo-sample");
    scanner.setProperty("sonar.analysis.mode", "incremental");

    thrown.expect(BuildFailureException.class);
    BuildResult res = orchestrator.executeBuild(scanner);

    assertThat(res.getLogs()).contains("Invalid analysis mode: incremental. This mode was removed in SonarQube 5.2");
  }

  @Test
  public void project_key_with_slash() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    setDefaultQualityProfile("xoo", "one-issue-per-line");

    SonarScanner scanner = configureScanner("shared/xoo-sample");
    scanner.setProjectKey("sample/project");
    scanner.setProperty("sonar.analysis.mode", "issues");
    BuildResult result = orchestrator.executeBuild(scanner);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);
  }

  // SONAR-6931
  @Test
  public void only_scan_changed_files_qps() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarScanner runner = configureScanner("shared/xoo-sample", "sonar.verbose", "true");
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
    runner = configureScannerIssues("shared/xoo-sample", null,
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

    SonarScanner runner = configureScanner("shared/xoo-sample", "sonar.verbose", "true");
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
    runner = configureScannerIssues("shared/xoo-sample", null,
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

    SonarScanner scanner = configureScanner("shared/xoo-sample", "sonar.verbose", "true");
    BuildResult result = orchestrator.executeBuild(scanner);

    // change QP
    restoreProfile("with-many-rules.xml");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");

    // now change file hash in a temporary location
    File tmpProjectDir = temp.newFolder();
    FileUtils.copyDirectory(ItUtils.projectDir("shared/xoo-sample"), tmpProjectDir);
    File srcFile = new File(tmpProjectDir, "src/main/xoo/sample/Sample.xoo");
    FileUtils.write(srcFile, "\n", StandardCharsets.UTF_8, true);

    // scan again, with different QP
    scanner = SonarScanner.create(tmpProjectDir,
      "sonar.working.directory", temp.newFolder().getAbsolutePath(),
      "sonar.analysis.mode", "issues",
      "sonar.report.export.path", "sonar-report.json",
      "sonar.userHome", temp.newFolder().getAbsolutePath(),
      "sonar.verbose", "true",
      "sonar.scanChangedFilesOnly", "true");
    result = orchestrator.executeBuild(scanner);
    assertThat(result.getLogs()).contains("Scanning only changed files");
    assertThat(result.getLogs()).doesNotContain("'One Issue Per Line' skipped because there is no related file in current project");
    ItUtils.assertIssuesInJsonReport(result, 3, 0, 17);
  }

  // SONAR-8518
  @Test
  public void should_support_sonar_profile_prop() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "empty");

    SonarScanner runner = configureScanner("shared/xoo-sample",
      "sonar.verbose", "true",
      "sonar.analysis.mode", "issues",
      "sonar.profile", "one-issue-per-line");
    BuildResult result = orchestrator.executeBuild(runner);
    ItUtils.assertIssuesInJsonReport(result, 17, 0, 0);
  }

  // SONAR-5715
  @Test
  public void test_issues_mode_on_project_with_space_in_filename() throws IOException {
    restoreProfile("with-many-rules.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample-with-spaces");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");

    SonarScanner runner = configureScanner("analysis/xoo-sample-with-spaces/v2");
    orchestrator.executeBuild(runner);
    assertThat(getComponent(orchestrator, "sample:my sources/main/xoo/sample/My Sample.xoo")).isNotNull();

    runner = configureScannerIssues("analysis/xoo-sample-with-spaces/v2", null);
    BuildResult result = orchestrator.executeBuild(runner);
    // Analysis is not persisted in database
    assertThat(getComponent(orchestrator, "com.sonarsource.it.samples:simple-sample")).isNull();
    assertThat(result.getLogs()).contains("Issues");
    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_not_fail_on_resources_that_have_existed_before() throws IOException {
    restoreProfile("with-many-rules.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-history");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "with-many-rules");

    // First real scan with source
    SonarScanner runner = configureScanner("shared/xoo-history-v2");
    orchestrator.executeBuild(runner);
    assertThat(getComponent(orchestrator, "sample:src/main/xoo/sample/ClassAdded.xoo")).isNotNull();

    // Second scan should remove ClassAdded.xoo
    runner = configureScanner("shared/xoo-history-v1");
    orchestrator.executeBuild(runner);
    assertThat(getComponent(orchestrator, "sample:src/main/xoo/sample/ClassAdded.xoo")).isNull();

    // Re-add ClassAdded.xoo in local workspace
    runner = configureScannerIssues("shared/xoo-history-v2", null);
    BuildResult result = orchestrator.executeBuild(runner);

    assertThat(getComponent(orchestrator, "sample:src/main/xoo/sample/ClassAdded.xoo")).isNull();
    assertThat(result.getLogs()).contains("Issues");
    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_fail_if_plugin_access_secured_properties() throws IOException {
    // Test access from task (ie BatchSettings)
    SonarScanner runner = configureScannerIssues("shared/xoo-sample", null,
      "accessSecuredFromTask", "true");
    BuildResult result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getLogs()).contains("Access to the secured property 'foo.bar.secured' is not possible in issues mode. "
      + "The SonarQube plugin which requires this property must be deactivated in issues mode.");

    // Test access from sensor (ie ModuleSettings)
    runner = configureScannerIssues("shared/xoo-sample", null,
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
    SonarScanner runner = configureScannerIssues("shared/xoo-sample", homeDir);
    BuildResult result = orchestrator.executeBuild(runner);

    // False positive is not returned
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(16);
  }

  private File runFirstAnalysisAndFlagIssueAsWontFix() throws IOException {
    restoreProfile("one-issue-per-line.xml");
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    // First run (publish mode)
    SonarScanner runner = configureScanner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    // First issues mode
    File homeDir = temp.newFolder();
    runner = configureScannerIssues("shared/xoo-sample", homeDir);
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
    SonarScanner runner = configureScanner("shared/xoo-sample");
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
    runner = configureScannerIssues("shared/xoo-sample", null, "sonar.login", "julien", "sonar.password", "password");
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
    assertThat(userNameByLogin.get("julien")).isEqualTo("julien");

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

    // use same working dir, because lock file is in it
    String workDirPath = temp.newFolder().getAbsolutePath();
    SonarScanner runner = configureScanner("shared/xoo-sample",
      "sonar.working.directory", workDirPath);

    orchestrator.executeBuild(runner);

    runConcurrentIssues(workDirPath);
  }

  private void runConcurrentIssues(final String workDirPath) throws Exception {
    // Install sonar-runner in advance to avoid concurrent unzip issues
    FileSystem fileSystem = orchestrator.getConfiguration().fileSystem();
    new SonarScannerInstaller(fileSystem).install(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION), fileSystem.workspace(), true);
    final int nThreads = 3;
    ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
    List<Callable<BuildResult>> tasks = new ArrayList<>();
    final File homeDir = temp.newFolder();
    for (int i = 0; i < nThreads; i++) {
      tasks.add(() -> {
        SonarScanner scanner = configureScannerIssues("shared/xoo-sample", homeDir,
          "sonar.it.enableWaitingSensor", "true",
          "sonar.working.directory", workDirPath);
        return orchestrator.executeBuild(scanner);
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
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/analysis/IssuesModeTest/" + fileName));
  }

  private SonarScanner configureScannerIssues(String projectDir, @Nullable File homeDir, String... props) throws IOException {
    SonarScanner scanner = SonarScanner.create(ItUtils.projectDir(projectDir),
      "sonar.working.directory", temp.newFolder().getAbsolutePath(),
      "sonar.analysis.mode", "issues",
      "sonar.report.export.path", "sonar-report.json");
    if (homeDir != null) {
      scanner.setProperty("sonar.userHome", homeDir.getAbsolutePath());
    } else {
      scanner.setProperty("sonar.userHome", temp.newFolder().getAbsolutePath());
    }
    scanner.setProperties(props);
    return scanner;
  }

  private SonarScanner configureScanner(String projectDir, String... props) throws IOException {
    SonarScanner scanner = SonarScanner.create(ItUtils.projectDir(projectDir),
      "sonar.working.directory", temp.newFolder().getAbsolutePath(),
      "sonar.report.export.path", "sonar-report.json",
      "sonar.userHome", temp.newFolder().getAbsolutePath());
    scanner.setProperties(props);
    return scanner;
  }

  private void setDefaultQualityProfile(String languageKey, String profileName) {
    tester.wsClient().wsConnector().call(new PostRequest("api/qualityprofiles/set_default")
      .setParam("language", languageKey)
      .setParam("profileName", profileName));
  }

}
