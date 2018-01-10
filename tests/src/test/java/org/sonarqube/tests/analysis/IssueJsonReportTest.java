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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.ResourceLocation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newWsClient;

public class IssueJsonReportTest {

  private static final String SONAR_VERSION_PLACEHOLDER = "<SONAR_VERSION>";
  private static final String RESOURCE_PATH = "/analysis/IssueJsonReportTest/";

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void issue_line() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(RESOURCE_PATH + "one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    File projectDir = ItUtils.projectDir("shared/xoo-sample");
    SonarScanner scanner = SonarScanner.create(projectDir,
      "sonar.analysis.mode", "issues",
      "sonar.verbose", "true",
      "sonar.report.export.path", "sonar-report.json");
    BuildResult result = orchestrator.executeBuild(scanner);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(17);

    JSONObject obj = ItUtils.getJSONReport(result);
    JSONArray issues = (JSONArray) obj.get("issues");
    for (Object issue : issues) {
      JSONObject jsonIssue = (JSONObject) issue;
      assertThat(jsonIssue.get("startLine")).isNotNull();
      assertThat(jsonIssue.get("line")).isEqualTo(jsonIssue.get("startLine"));
      assertThat(jsonIssue.get("endLine")).isEqualTo(jsonIssue.get("startLine"));

      assertThat(jsonIssue.get("endOffset")).isNotNull();
      assertThat(jsonIssue.get("startOffset")).isNotNull();
    }

    List<Long> lineNumbers = new ArrayList<>(16);
    for (long i = 1L; i < 18; i++) {
      lineNumbers.add(i);
    }
    assertThat(issues).extracting("startLine").containsAll(lineNumbers);
    assertThat(issues).extracting("endLine").containsAll(lineNumbers);
  }

  @Test
  public void precise_issue_location() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(RESOURCE_PATH + "multiline.xml"));
    orchestrator.getServer().provisionProject("sample-multiline", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample-multiline", "xoo", "multiline");

    File projectDir = ItUtils.projectDir("shared/xoo-precise-issues");
    SonarScanner scanner = SonarScanner.create(projectDir,
      "sonar.analysis.mode", "issues",
      "sonar.verbose", "true",
      "sonar.report.export.path", "sonar-report.json");
    BuildResult result = orchestrator.executeBuild(scanner);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(2);

    JSONObject obj = ItUtils.getJSONReport(result);
    JSONArray issues = (JSONArray) obj.get("issues");

    JSONObject issue1 = (JSONObject) issues.get(0);
    JSONObject issue2 = (JSONObject) issues.get(1);

    assertThat(issue1.get("startLine")).isIn(6L);
    assertThat(issue1.get("line")).isIn(6L);
    assertThat(issue1.get("endLine")).isIn(6L);
    assertThat(issue1.get("startOffset")).isIn(27L);
    assertThat(issue1.get("endOffset")).isIn(32L);

    assertThat(issue2.get("startLine")).isIn(9L);
    assertThat(issue2.get("line")).isIn(9L);
    assertThat(issue2.get("endLine")).isIn(15L);
    assertThat(issue2.get("startOffset")).isIn(20L);
    assertThat(issue2.get("endOffset")).isIn(2L);

  }

  @Test
  public void test_json_report_no_server_analysis() throws Exception {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(RESOURCE_PATH + "one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "tracking");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    File projectDir = ItUtils.projectDir("analysis/tracking/v1");
    SonarScanner issuesModeScan = SonarScanner.create(projectDir)
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(projectDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = expected("no-server-analysis.json");
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void test_json_report() throws Exception {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(RESOURCE_PATH + "one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "tracking");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarScanner scan = SonarScanner.create(ItUtils.projectDir("analysis/tracking/v1"))
      .setProperty("sonar.projectDate", "2013-05-01");
    orchestrator.executeBuild(scan);

    // Issues mode scan -> 2 new issues and 13 existing issues
    File projectDir = ItUtils.projectDir("analysis/tracking/v2");
    SonarScanner issuesModeScan = SonarScanner.create(projectDir)
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(projectDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = expected("report-on-single-module.json");
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void test_json_report_on_branch() throws Exception {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(getResource("one-issue-per-line.xml").getPath()));
    orchestrator.getServer().provisionProject("sample:mybranch", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample:mybranch", "xoo", "one-issue-per-line");

    SonarScanner scan = SonarScanner.create(ItUtils.projectDir("analysis/tracking/v1"))
      .setProperty("sonar.projectDate", "2013-05-01")
      .setProperty("sonar.branch", "mybranch");
    orchestrator.executeBuild(scan);

    // issues mode scan -> 2 new issues and 13 existing issues
    File projectDir = ItUtils.projectDir("analysis/tracking/v2");
    SonarScanner issuesModeScan = SonarScanner.create(projectDir)
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.issuesReport.console.enable", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      .setProperty("sonar.verbose", "true")
      .setProperty("sonar.branch", "mybranch");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(projectDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = expected("report-on-single-module-branch.json");
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project but Eclipse scans only a single module
   */
  @Test
  public void test_json_report_on_sub_module() throws Exception {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(RESOURCE_PATH + "one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Multi-module sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    File rootDir = ItUtils.projectDir("shared/xoo-multi-modules-sample");
    SonarScanner scan = SonarScanner.create(rootDir)
      .setProperty("sonar.projectDate", "2013-05-01");
    orchestrator.executeBuild(scan);

    // Issues mode scan on a module -> no new issues
    File moduleDir = ItUtils.projectDir("shared/xoo-multi-modules-sample/module_a/module_a1");
    SonarScanner issuesModeScan = SonarScanner.create(moduleDir)
      .setProperty("sonar.projectKey", "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.projectName", "ModuleA1")
      .setProperty("sonar.sources", "src/main/xoo")
      .setProperty("sonar.language", "xoo")
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(moduleDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    // SONAR-5218 All issues are updated as their root project id has changed (it's now the sub module)
    String expectedJson = expected("report-on-sub-module.json");
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project
   */
  @Test
  public void test_json_report_on_root_module() throws Exception {
    ItUtils.restoreProfile(orchestrator, getClass().getResource(RESOURCE_PATH + "one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    File rootDir = ItUtils.projectDir("shared/xoo-multi-modules-sample");
    SonarScanner scan = SonarScanner.create(rootDir)
      .setProperty("sonar.projectDate", "2013-05-01");
    orchestrator.executeBuild(scan);

    // issues mode scan -> no new issues
    SonarScanner issuesModeScan = SonarScanner.create(rootDir)
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(rootDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = expected("report-on-root-module.json");
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  private String expected(String path) throws IOException {
    try (WsResponse response = newWsClient(orchestrator).wsConnector().call(new GetRequest("api/server/version"))) {
      String version = response.content();

      return sanitize(IOUtils.toString(getResourceInputStream(path)))
        .replaceAll(Pattern.quote(SONAR_VERSION_PLACEHOLDER), version);
    }
  }

  @Test
  public void issueSanityCheck() {
    assertThat(sanitize("s\"0150F1EBDB8E000003\"f")).isEqualTo("s<ISSUE_KEY>f");
  }

  private static String sanitize(String s) {
    // sanitize issue uuid keys
    s = s.replaceAll("\"[a-zA-Z_0-9\\-]{15,20}\"", "<ISSUE_KEY>");

    return ItUtils.sanitizeTimezones(s);
  }

  private InputStream getResourceInputStream(String resource) {
    ResourceLocation res = getResource(resource);
    return getClass().getResourceAsStream(res.getPath());
  }

  private ResourceLocation getResource(String resource) {
    return FileLocation.ofClasspath(RESOURCE_PATH + resource);
  }

}
