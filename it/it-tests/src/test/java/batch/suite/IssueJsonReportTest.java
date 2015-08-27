/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch.suite;

import com.sonar.orchestrator.locator.ResourceLocation;
import util.ItUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class IssueJsonReportTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void test_json_report_no_server_analysis() throws Exception {
    orchestrator.getServer().restoreProfile(getResource("one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "tracking");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    File projectDir = ItUtils.projectDir("batch/tracking/v1");
    SonarRunner issuesModeScan = SonarRunner.create(projectDir)
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(projectDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getResourceInputStream("no-server-analysis.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void test_json_report() throws Exception {
    orchestrator.getServer().restoreProfile(getResource("one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "tracking");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarRunner scan = SonarRunner.create(ItUtils.projectDir("batch/tracking/v1"))
      .setProperty("sonar.projectDate", "2013-05-01");
    orchestrator.executeBuild(scan);

    // Issues mode scan -> 2 new issues and 13 existing issues
    File projectDir = ItUtils.projectDir("batch/tracking/v2");
    SonarRunner issuesModeScan = SonarRunner.create(projectDir)
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(projectDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getResourceInputStream("report-on-single-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void test_json_report_on_branch() throws Exception {
    orchestrator.getServer().restoreProfile(getResource("one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample:mybranch", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample:mybranch", "xoo", "one-issue-per-line");

    SonarRunner scan = SonarRunner.create(ItUtils.projectDir("batch/tracking/v1"))
      .setProperty("sonar.projectDate", "2013-05-01")
      .setProperty("sonar.branch", "mybranch");
    orchestrator.executeBuild(scan);

    // issues mode scan -> 2 new issues and 13 existing issues
    File projectDir = ItUtils.projectDir("batch/tracking/v2");
    SonarRunner issuesModeScan = SonarRunner.create(projectDir)
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
    String expectedJson = sanitize(IOUtils.toString(getResourceInputStream("report-on-single-module-branch.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project but Eclipse scans only a single module
   */
  @Test
  public void test_json_report_on_sub_module() throws Exception {
    orchestrator.getServer().restoreProfile(getResource("one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Multi-module sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    File rootDir = ItUtils.projectDir("shared/xoo-multi-modules-sample");
    SonarRunner scan = SonarRunner.create(rootDir)
      .setProperty("sonar.projectDate", "2013-05-01");
    orchestrator.executeBuild(scan);

    // Issues mode scan on a module -> no new issues
    File moduleDir = ItUtils.projectDir("shared/xoo-multi-modules-sample/module_a/module_a1");
    SonarRunner issuesModeScan = SonarRunner.create(moduleDir)
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
    String expectedJson = sanitize(IOUtils.toString(getResourceInputStream("report-on-sub-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project
   */
  @Test
  public void test_json_report_on_root_module() throws Exception {
    orchestrator.getServer().restoreProfile(getResource("/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    File rootDir = ItUtils.projectDir("shared/xoo-multi-modules-sample");
    SonarRunner scan = SonarRunner.create(rootDir)
      .setProperty("sonar.projectDate", "2013-05-01");
    orchestrator.executeBuild(scan);

    // issues mode scan -> no new issues
    SonarRunner issuesModeScan = SonarRunner.create(rootDir)
      .setProperty("sonar.analysis.mode", "issues")
      .setProperty("sonar.userHome", temp.newFolder().getAbsolutePath())
      .setProperty("sonar.report.export.path", "sonar-report.json")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(issuesModeScan);

    File report = new File(rootDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getResourceInputStream("report-on-root-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void sanityCheck() {
    assertThat(sanitize("5.0.0-5868-SILVER-SNAPSHOT")).isEqualTo("<SONAR_VERSION>");
  }

  private static String sanitize(String s) {
    // sanitize issue uuid keys
    s = s.replaceAll("\"[a-zA-Z_0-9\\-]{20}\"", "<ISSUE_KEY>");

    // sanitize sonar version. Note that "-SILVER-SNAPSHOT" is used by Goldeneye jobs
    s = s.replaceAll("\\d\\.\\d(.\\d)?(\\-.*)?\\-SNAPSHOT", "<SONAR_VERSION>");

    return ItUtils.sanitizeTimezones(s);
  }

  private InputStream getResourceInputStream(String resource) throws FileNotFoundException {
    ResourceLocation res = getResource(resource);
    return getClass().getResourceAsStream(res.getPath());
  }

  private ResourceLocation getResource(String resource) {
    return FileLocation.ofClasspath("/batch/IssueJsonReportTest/" + resource);
  }

}
