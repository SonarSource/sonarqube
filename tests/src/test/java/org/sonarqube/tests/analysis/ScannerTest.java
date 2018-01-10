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
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.ws.client.components.SearchRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getComponent;
import static util.ItUtils.getComponentNavigation;
import static util.ItUtils.getMeasureAsDouble;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.resetSettings;

public class ScannerTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  public void setUp() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/analysis/BatchTest/one-issue-per-line.xml"));
  }

  /**
   * SONAR-3718
   */
  @Test
  public void should_scan_branch_with_forward_slash() {
    scan("shared/xoo-multi-modules-sample");
    scan("shared/xoo-multi-modules-sample", "sonar.branch", "branch/0.x");

    assertThat(tester.wsClient().components().search(new SearchRequest().setQualifiers(singletonList("TRK"))).getComponentsList()).hasSize(2);
    assertThat(getComponent(orchestrator, "com.sonarsource.it.samples:multi-modules-sample").getName()).isEqualTo("Sonar :: Integration Tests :: Multi-modules Sample");
    assertThat(getComponent(orchestrator, "com.sonarsource.it.samples:multi-modules-sample:branch/0.x").getName())
      .isEqualTo("Sonar :: Integration Tests :: Multi-modules Sample branch/0.x");
  }

  @Test
  public void use_sonar_profile_without_provisioning_project() {
    scan("shared/xoo-multi-modules-sample",
      "sonar.profile", "one-issue-per-line",
      "sonar.verbose", "true");
    assertThat(getMeasureAsDouble(orchestrator, "com.sonarsource.it.samples:multi-modules-sample", "violations")).isEqualTo(61);
  }

  // SONAR-4680
  @Test
  public void module_should_load_own_settings_from_database() {
    String rootModuleKey = "com.sonarsource.it.samples:multi-modules-sample";
    orchestrator.getServer().provisionProject(rootModuleKey, "Sonar :: Integration Tests :: Multi-modules Sample");

    String propKey = "myFakeProperty";
    String moduleBKey = rootModuleKey + ":module_b";
    resetSettings(orchestrator, rootModuleKey, propKey);

    BuildResult result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", propKey);

    assertThat(result.getLogs()).doesNotContain(rootModuleKey + ":" + propKey);
    assertThat(result.getLogs()).doesNotContain(moduleBKey + ":" + propKey);

    // Set property only on root project
    tester.settings().setProjectSetting(rootModuleKey, propKey, "project");

    result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", propKey);

    assertThat(result.getLogs()).contains(rootModuleKey + ":" + propKey + " = project");
    assertThat(result.getLogs()).contains(moduleBKey + ":" + propKey + " = project");

    // Override property on moduleB
    tester.settings().setProjectSetting(moduleBKey, propKey, "moduleB");

    result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", propKey);

    assertThat(result.getLogs()).contains(rootModuleKey + ":" + propKey + " = project");
    assertThat(result.getLogs()).contains(moduleBKey + ":" + propKey + " = moduleB");
  }

  // SONAR-4680
  @Test
  public void module_should_load_settings_from_parent() {
    String rootModuleKey = "com.sonarsource.it.samples:multi-modules-sample";
    orchestrator.getServer().provisionProject(rootModuleKey, "Sonar :: Integration Tests :: Multi-modules Sample");

    String propKey = "myFakeProperty";
    String moduleBKey = rootModuleKey + ":module_b";

    // Set property on provisioned project
    tester.settings().setProjectSetting(rootModuleKey, propKey, "project");

    BuildResult result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", propKey);

    assertThat(result.getLogs()).contains(rootModuleKey + ":" + propKey + " = project");
    // Module should inherit from parent
    assertThat(result.getLogs()).contains(moduleBKey + ":" + propKey + " = project");
  }

  /**
   * SONAR-3024
   */
  @Test
  public void should_support_source_files_with_same_deprecated_key() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.projects.batch:duplicate-source", "exclusions");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.projects.batch:duplicate-source", "xoo", "one-issue-per-line");
    scan("analysis/duplicate-source");

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "com.sonarsource.it.projects.batch:duplicate-source", "files", "directories");
    // 2 main files and 1 test file all with same deprecated key
    assertThat(measures.get("files")).isEqualTo(2);
    assertThat(measures.get("directories")).isEqualTo(2);
  }

  /**
   * SONAR-3125
   */
  @Test
  public void should_display_explicit_message_when_no_plugin_language_available() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.language", "foo",
      "sonar.profile", "");
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "You must install a plugin that supports the language 'foo'");
  }

  @Test
  public void should_display_explicit_message_when_wrong_profile() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.profile", "unknow");
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "sonar.profile was set to 'unknow' but didn't match any profile for any language. Please check your configuration.");
  }

  @Test
  public void should_create_project_without_name_version() {
    // some of the sub-modules have a name defined, others don't
    BuildResult buildResult = scan("shared/xoo-multi-module-sample-without-project-name-version");
    assertThat(buildResult.isSuccess()).isTrue();

    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample", "com.sonarsource.it.samples:multi-modules-sample", "not provided");

    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_b", "module_b", "not provided");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b1", "module_b1", "not provided");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b2", "Sub-module B2", "not provided");

    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_a", "Module A", "not provided");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1", "Sub-module A1", "not provided");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a2", "Sub-module A2", "not provided");
  }

  @Test
  public void should_analyze_project_without_name_version() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "My project name");
    BuildResult buildResult = scan("shared/xoo-multi-module-sample-without-project-name-version",
      "sonar.projectName", "My project name",
      "sonar.projectVersion", "1.0");
    assertThat(buildResult.isSuccess()).isTrue();

    buildResult = scan("shared/xoo-multi-module-sample-without-project-name-version");
    assertThat(buildResult.isSuccess()).isTrue();

    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample", "My project name", "1.0");

    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_b", "module_b", "1.0");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b1", "module_b1", "1.0");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_b:module_b2", "Sub-module B2", "1.0");

    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_a", "Module A", "1.0");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1", "Sub-module A1", "1.0");
    assertNameAndVersion("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a2", "Sub-module A2", "1.0");
  }

  private void assertNameAndVersion(String projectKey, String expectedProjectName, String expectedProjectVersion) {
    assertThat(getComponent(orchestrator, projectKey).getName()).isEqualTo(expectedProjectName);
    assertThat(getComponentNavigation(orchestrator, projectKey).getVersion()).isEqualTo(expectedProjectVersion);
  }

  @Test
  public void should_honor_sonarUserHome() {
    File userHome = temp.getRoot();

    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    SonarScanner scanner = configureScanner("shared/xoo-sample",
      "sonar.verbose", "true");
    scanner.setEnvironmentVariable("SONAR_USER_HOME", "/dev/null");
    BuildResult buildResult = orchestrator.executeBuildQuietly(scanner);
    assertThat(buildResult.getLastStatus()).isEqualTo(1);

    buildResult = scan("shared/xoo-sample",
      "sonar.verbose", "true",
      "sonar.userHome", userHome.getAbsolutePath());
    assertThat(buildResult.isSuccess()).isTrue();
  }

  /**
   * SONAR-2291
   */
  @Test
  public void scanner_should_cache_plugin_jars() throws IOException {
    File userHome = temp.newFolder();

    BuildResult result = scan("shared/xoo-sample",
      "sonar.userHome", userHome.getAbsolutePath());

    File cache = new File(userHome, "cache");
    assertThat(cache).exists().isDirectory();
    int cachedFiles = FileUtils.listFiles(cache, new String[]{"jar"}, true).size();
    assertThat(cachedFiles).isGreaterThan(5);
    assertThat(result.getLogs()).contains("User cache: " + cache.getAbsolutePath());

    result = scan("shared/xoo-sample",
      "sonar.userHome", userHome.getAbsolutePath());
    int cachedFiles2 = FileUtils.listFiles(cache, new String[]{"jar"}, true).size();
    assertThat(cachedFiles).isEqualTo(cachedFiles2);
    assertThat(result.getLogs()).contains("User cache: " + cache.getAbsolutePath());
  }

  @Test
  public void scanner_should_keep_report_verbose() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    scanQuietly("shared/xoo-sample", "sonar.verbose", "true");
    File reportDir = new File(new File(ItUtils.projectDir("shared/xoo-sample"), ".sonar"), "scanner-report");
    assertThat(reportDir).isDirectory();
    assertThat(reportDir.list()).isNotEmpty();
  }

  /**
   * SONAR-4239
   */
  @Test
  public void should_display_project_url_after_analysis() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    BuildResult result = scan("shared/xoo-multi-modules-sample");

    assertThat(result.getLogs()).contains("/dashboard/index/com.sonarsource.it.samples:multi-modules-sample");

    result = scan("shared/xoo-multi-modules-sample",
      "sonar.branch", "mybranch");

    assertThat(result.getLogs()).contains("/dashboard/index/com.sonarsource.it.samples:multi-modules-sample:mybranch");

    tester.settings().setGlobalSettings("sonar.core.serverBaseURL", "http://foo:123/sonar");
    result = scan("shared/xoo-multi-modules-sample");
    assertThat(result.getLogs()).contains("http://foo:123/sonar/dashboard/index/com.sonarsource.it.samples:multi-modules-sample");
  }

  /**
   * SONAR-4188, SONAR-5178, SONAR-5915
   */
  @Test
  public void should_display_explicit_message_when_invalid_project_key_or_branch() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.projectKey", "ar g$l:");
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"ar g$l:\" is not a valid project or module key")
      .contains("Allowed characters");

    // SONAR-4629
    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.projectKey", "12345");
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"12345\" is not a valid project or module key")
      .contains("Allowed characters");

    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.branch", "ar g$l:");
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"ar g$l:\" is not a valid branch")
      .contains("Allowed characters");
  }

  @Test
  public void display_explicit_message_when_using_existing_module_key_as_project_key() {
    String projectKey = "com.sonarsource.it.samples:multi-modules-sample";
    String moduleKey = "com.sonarsource.it.samples:multi-modules-sample:module_a";
    scan("shared/xoo-multi-modules-sample", "sonar.projectKey", projectKey);

    BuildResult buildResult = scanQuietly("shared/xoo-sample", "sonar.projectKey", moduleKey);
    assertThat(buildResult.getLastStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs())
      .contains(String.format("Component '%s' is not a project", moduleKey))
      .contains(String.format("The project '%s' is already defined in SonarQube but as a module of project '%s'. If you really want to stop directly analysing project '%s', " +
        "please first delete it from SonarQube and then relaunch the analysis of project '%s'", moduleKey, projectKey, projectKey, moduleKey));
  }

  /**
   * SONAR-4547
   */
  @Test
  public void display_MessageException_without_stacktrace() {
    orchestrator.getServer().provisionProject("sample", "xoo-sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    BuildResult result = scanQuietly("shared/xoo-sample", "raiseMessageException", "true");
    assertThat(result.getLastStatus()).isNotEqualTo(0);
    assertThat(result.getLogs())
      // message
      .contains("Error message from plugin")

      // but not stacktrace
      .doesNotContain("at com.sonarsource.RaiseMessageException");
  }

  /**
   * SONAR-4751
   */
  @Test
  public void file_extensions_are_case_insensitive() {
    orchestrator.getServer().provisionProject("case-sensitive-file-extensions", "Case Sensitive");
    orchestrator.getServer().associateProjectToQualityProfile("case-sensitive-file-extensions", "xoo", "one-issue-per-line");
    scan("analysis/case-sensitive-file-extensions");

    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, "case-sensitive-file-extensions", "files", "ncloc");
    assertThat(measures.get("files")).isEqualTo(2);
    assertThat(measures.get("ncloc")).isEqualTo(5 + 2);
  }

  /**
   * SONAR-4876
   */
  @Test
  public void custom_module_key() {
    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Sonar :: Integration Tests :: Multi-modules Sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");
    scan("analysis/custom-module-key");
    assertThat(getComponent(orchestrator, "com.sonarsource.it.samples:moduleA")).isNotNull();
    assertThat(getComponent(orchestrator, "com.sonarsource.it.samples:moduleB")).isNotNull();
  }

  private BuildResult scan(String projectPath, String... props) {
    SonarScanner scanner = configureScanner(projectPath, props);
    return orchestrator.executeBuild(scanner);
  }

  private BuildResult scanQuietly(String projectPath, String... props) {
    SonarScanner scanner = configureScanner(projectPath, props);
    return orchestrator.executeBuildQuietly(scanner);
  }

  private SonarScanner configureScanner(String projectPath, String... props) {
    return SonarScanner.create(ItUtils.projectDir(projectPath))
      .setProperties(props);
  }

}
