/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.issue;

import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.QaOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperties;

@Category(QaOnly.class)
public class IssueFilterTest extends AbstractIssueTest {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-exclusions";
  private static final String PROJECT_DIR = "exclusions/xoo-multi-modules";

  @Before
  public void resetData() {
    ORCHESTRATOR.resetData();

    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/IssueFilterTest/with-many-rules.xml"));
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, "project");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "with-many-rules");
  }

  @Test
  public void ignore_all_files() {
    executeAnalysis(
      "sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "**/*.xoo",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "*");

    checkIssueCountBySeverity(4, 0, 0, 4);
  }

  @Test
  public void enforce_only_on_one_file() {
    executeAnalysis(
      "sonar.issue.enforce.multicriteria", "1",
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/HelloA1.xoo",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "*");

    checkIssueCountBySeverity(
      1 /* tag */ + 18 /* lines in HelloA1.xoo */ + 1 /* file */,
      0 + 1,
      0,
      0);
  }

  @Test
  public void enforce_on_two_files_with_same_rule() {
    executeAnalysis(
      "sonar.issue.enforce.multicriteria", "1,2",
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/HelloA1.xoo",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "*",
      "sonar.issue.enforce.multicriteria.2.resourceKey", "**/HelloA2.xoo",
      "sonar.issue.enforce.multicriteria.2.ruleKey", "*");

    checkIssueCountBySeverity(
      2 /* tags */ + 18 /* lines in HelloA1.xoo */ + 15 /* lines in HelloA2.xoo */ + 2 /* files */,
      0 + 2,
      0,
      0);
  }

  @Test
  public void enforce_on_two_files_with_different_rules() {
    executeAnalysis(
      "sonar.issue.enforce.multicriteria", "1,2",
      "sonar.issue.enforce.multicriteria.1.resourceKey", "**/HelloA1.xoo",
      "sonar.issue.enforce.multicriteria.1.ruleKey", "xoo:OneIssuePerLine",
      "sonar.issue.enforce.multicriteria.2.resourceKey", "**/HelloA2.xoo",
      "sonar.issue.enforce.multicriteria.2.ruleKey", "xoo:HasTag");

    checkIssueCountBySeverity(
      1 /* tag in HelloA2 */ + 18 /* lines in HelloA1.xoo */ + 4 /* files */ + 4 /* modules */,
      4,
      0,
      4);
  }

  private void executeAnalysis(String... serverProperties) {
    setServerProperties(ORCHESTRATOR, PROJECT_KEY, serverProperties);
    runProjectAnalysis(ORCHESTRATOR, PROJECT_DIR);
  }

  private void checkIssueCountBySeverity(int total, int perFile, int perCommonRule, int perModule) {
    Resource project = ORCHESTRATOR.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(PROJECT_KEY, "violations", "major_violations", "blocker_violations", "critical_violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(total);
    assertThat(project.getMeasureIntValue("major_violations")).isEqualTo(perFile); // One per file
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(perCommonRule); // On per common rule
                                                                                           // 'InsufficientCommentDensity'
    assertThat(project.getMeasureIntValue("critical_violations")).isEqualTo(perModule); // One per module
  }

}
