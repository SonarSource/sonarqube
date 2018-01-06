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
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.ws.client.qualityprofiles.AddProjectRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;

public class IssueExclusionsTest {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-exclusions";
  private static final String PROJECT_DIR = "exclusions/xoo-multi-modules";

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of Category4Suite must disable organizations
    .disableOrganizations();

  @Test
  public void should_not_exclude_anything() {
    scan();

    checkIssueCountBySeverity(67, 2, 57, 4, 0, 4);
  }

  @Test
  public void should_ignore_all_files() {
    scan(
      "sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "**/*.xoo",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "*");

    checkIssueCountBySeverity(4, 0, 0, 0, 0, 4);
  }

  @Test
  public void should_enforce_only_on_one_file() {
    scan(
      "sonar.issue.enforce.multicriteria", "1",
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/HelloA1.xoo",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "*");

    checkIssueCountBySeverity(
      1 /* tag */ + 18 /* lines in HelloA1.xoo */ + 1 /* file */,
      0 + 1,
      0 + 18,
      0 + 1,
      0,
      0);
  }

  @Test
  public void should_enforce_on_two_files_with_same_rule() {
    scan(
      "sonar.issue.enforce.multicriteria", "1,2",
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/HelloA1.xoo",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "*",
      "sonar.issue.enforce.multicriteria.2.resourceKey", "**/HelloA2.xoo",
      "sonar.issue.enforce.multicriteria.2.ruleKey", "*");

    checkIssueCountBySeverity(
      2 /* tags */ + 18 /* lines in HelloA1.xoo */ + 15 /* lines in HelloA2.xoo */ + 2 /* files */,
      0 + 2,
      0 + 18 + 15,
      0 + 2,
      0,
      0);
  }

  @Test
  public void should_enforce_on_two_files_with_different_rule() {
    scan(
      "sonar.issue.enforce.multicriteria", "1,2",
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/HelloA1.xoo",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "xoo:OneIssuePerLine",
      "sonar.issue.enforce.multicriteria.2.resourceKey", "**/HelloA2.xoo",
      "sonar.issue.enforce.multicriteria.2.ruleKey", "xoo:HasTag");

    checkIssueCountBySeverity(
      1 /* tag in HelloA2 */ + 18 /* lines in HelloA1.xoo */ + 4 /* files */ + 4 /* modules */,
      0 + 1,
      0 + 18,
      4,
      0,
      4);
  }

  @Test
  public void should_ignore_files_with_regexp() {
    scan(
      "sonar.issue.ignore.allfile", "1",
      "sonar.issue.ignore.allfile.1.fileRegexp", "EXTERMINATE-ALL-ISSUES");

    checkIssueCountBySeverity(
      67 - 1 /* tag */ - 18 /* lines in HelloA1.xoo */ - 1 /* file */,
      2 - 1,
      57 - 18,
      4 - 1,
      0,
      4);
  }

  @Test
  public void should_ignore_block_with_regexp() {
    scan(
      "sonar.issue.ignore.block", "1",
      "sonar.issue.ignore.block.1.beginBlockRegexp", "MUTE-SONAR",
      "sonar.issue.ignore.block.1.endBlockRegexp", "UNMUTE-SONAR");

    checkIssueCountBySeverity(
      67 - 1 /* tag */ - 5 /* lines in HelloA2.xoo */,
      2 - 1,
      57 - 5,
      4,
      0,
      4);
  }

  @Test
  public void should_ignore_to_end_of_file() {
    scan(
      "sonar.issue.ignore.block", "1",
      "sonar.issue.ignore.block.1.beginBlockRegexp", "MUTE-SONAR",
      "sonar.issue.ignore.block.1.endBlockRegexp", "");

    checkIssueCountBySeverity(
      67 - 1 /* tag */ - 7 /* remaining lines in HelloA2.xoo */,
      2 - 1,
      57 - 7,
      4,
      0,
      4);
  }

  @Test
  public void should_ignore_one_per_line_on_single_package() {
    scan(
      "sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "**/com/sonar/it/samples/modules/a1/*",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "xoo:OneIssuePerLine");

    checkIssueCountBySeverity(
      67 - 18 /* lines in HelloA1.xoo */,
      2,
      57 - 18,
      4,
      0,
      4);
  }

  @Test
  public void should_apply_exclusions_from_multiple_sources() {
    scan(
      "sonar.issue.ignore.allfile", "1",
      "sonar.issue.ignore.allfile.1.fileRegexp", "EXTERMINATE-ALL-ISSUES",
      "sonar.issue.ignore.block", "1",
      "sonar.issue.ignore.block.1.beginBlockRegexp", "MUTE-SONAR",
      "sonar.issue.ignore.block.1.endBlockRegexp", "UNMUTE-SONAR",
      "sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "**/com/sonar/it/samples/modules/b1/*",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "xoo:OneIssuePerLine");

    checkIssueCountBySeverity(
      67 - 1 /* tag in HelloA1.xoo */ - 1 /* tag in HelloA2.xoo */
        - 18 /* lines in HelloA1.xoo */ - 5 /* lines in HelloA2.xoo */ - 12 /* lines in HelloB1.xoo */
        - 1 /* HelloA1.xoo file */,
      0,
      57 - 18 - 5 - 12,
      4 - 1,
      0,
      4);
  }

  @Test
  public void should_log_missing_resource_key() {
    checkAnalysisFails(
      "sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "*");
  }

  @Test
  public void should_log_missing_rule_key() {
    checkAnalysisFails(
      "sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "*",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "");
  }

  @Test
  public void should_log_missing_block_start() {
    checkAnalysisFails(
      "sonar.issue.ignore.block", "1",
      "sonar.issue.ignore.block.1.beginBlockRegexp", "",
      "sonar.issue.ignore.block.1.endBlockRegexp", "UNMUTE-SONAR");
  }

  @Test
  public void should_log_missing_whole_file_regexp() {
    checkAnalysisFails(
      "sonar.issue.ignore.allfile", "1",
      "sonar.issue.ignore.allfile.1.fileRegexp", "");
  }

  protected BuildResult scan(String... properties) {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/exclusions/IssueExclusionsTest/with-many-rules.xml"));

    tester.projects().provision(p -> p
      .setProject("com.sonarsource.it.samples:multi-modules-exclusions")
      .setName("Sonar :: Integration Tests :: Multi-modules With Exclusions"));
    tester.wsClient().qualityprofiles().addProject(new AddProjectRequest().setProject("com.sonarsource.it.samples:multi-modules-exclusions")
      .setLanguage("xoo").setQualityProfile("with-many-rules"));

    SonarScanner scan = SonarScanner.create(ItUtils.projectDir(PROJECT_DIR))
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setProperties(properties)
      .setProperty("sonar.verbose", "true");
    return orchestrator.executeBuildQuietly(scan);
  }

  private void checkIssueCountBySeverity(int total, int taggedXoo, int perLine, int perFile, int blocker, int perModule) {
    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, PROJECT_KEY, "violations", "info_violations", "minor_violations", "major_violations",
      "blocker_violations", "critical_violations");
    assertThat(measures.get("violations").intValue()).isEqualTo(total);
    assertThat(measures.get("info_violations").intValue()).isEqualTo(taggedXoo); // Has tag 'xoo'
    assertThat(measures.get("minor_violations").intValue()).isEqualTo(perLine); // One per line
    assertThat(measures.get("major_violations").intValue()).isEqualTo(perFile); // One per file
    assertThat(measures.get("blocker_violations").intValue()).isEqualTo(blocker);
    assertThat(measures.get("critical_violations").intValue()).isEqualTo(perModule); // One per module
  }

  private void checkAnalysisFails(String... properties) {
    BuildResult buildResult = scan(properties);
    assertThat(buildResult.getStatus()).isNotEqualTo(0);
    assertThat(buildResult.getLogs().indexOf("IllegalStateException")).isGreaterThan(0);
  }
}
