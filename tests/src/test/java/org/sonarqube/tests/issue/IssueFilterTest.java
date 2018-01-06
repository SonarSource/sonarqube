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
package org.sonarqube.tests.issue;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperties;

public class IssueFilterTest extends AbstractIssueTest {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-exclusions";
  private static final String PROJECT_DIR = "exclusions/xoo-multi-modules";

  @Before
  public void resetData() {
    ORCHESTRATOR.resetData();

    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueFilterTest/with-many-rules.xml"));
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
    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(ORCHESTRATOR, PROJECT_KEY, "violations", "major_violations", "blocker_violations", "critical_violations");
    assertThat(measures.get("violations").intValue()).isEqualTo(total);
    assertThat(measures.get("major_violations").intValue()).isEqualTo(perFile); // One per file
    assertThat(measures.get("blocker_violations").intValue()).isEqualTo(perCommonRule); // On per common rule
    assertThat(measures.get("critical_violations").intValue()).isEqualTo(perModule); // One per module
  }

}
