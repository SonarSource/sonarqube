/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.measure.ws;

import org.junit.jupiter.api.Test;
import org.sonar.core.platform.ListContainer;

import static org.assertj.core.api.Assertions.assertThat;

class MeasuresWsModuleTest {
  @Test
  void verify_count_of_added_components() {
    ListContainer container = new ListContainer();
    new MeasuresWsModule().configure(container);
    assertThat(container.getAddedObjects()).hasSize(7);
  }

  @Test
  void getDeprecatedMetricsInSonarQube104_shouldReturnExactString() {
    String actual = MeasuresWsModule.getDeprecatedMetricsInSonarQube104();

    assertThat(actual).isEqualTo("'bugs', 'new_bugs', 'vulnerabilities', 'new_vulnerabilities', 'code_smells', 'new_code_smells', " +
      "'high_impact_accepted_issues'");
  }

  @Test
  void getDeprecatedMetricsInSonarQube105_shouldReturnExactString() {
    String actual = MeasuresWsModule.getDeprecatedMetricsInSonarQube105();

    assertThat(actual).isEqualTo("'new_blocker_violations', 'new_critical_violations', 'new_major_violations', 'new_minor_violations', " +
      "'new_info_violations', 'blocker_violations', 'critical_violations', 'major_violations', 'minor_violations', 'info_violations'");
  }

  @Test
  void getNewMetricsInSonarQube107_shouldReturnExactString() {
    String actual = MeasuresWsModule.getNewMetricsInSonarQube107();
    assertThat(actual).isEqualTo("'software_quality_maintainability_debt_ratio', 'software_quality_maintainability_rating', 'software_quality_reliability_rating', " +
      "'software_quality_security_rating', 'software_quality_maintainability_remediation_effort', " +
      "'software_quality_reliability_remediation_effort', 'software_quality_security_remediation_effort', 'effort_to_reach_software_quality_maintainability_rating_a', " +
      "'new_software_quality_maintainability_debt_ratio', 'new_software_quality_maintainability_rating', 'new_software_quality_reliability_rating', " +
      "'new_software_quality_security_rating', 'new_software_quality_maintainability_remediation_effort'," +
      " 'new_software_quality_reliability_remediation_effort', 'new_software_quality_security_remediation_effort'");
  }

  @Test
  void getDeprecatedMetricsInSonarQube108_shouldReturnExactString() {
    String actual = MeasuresWsModule.getDeprecatedMetricsInSonarQube108();
    assertThat(actual).isEqualTo("'maintainability_issues', 'reliability_issues', 'security_issues', " +
      "'new_maintainability_issues', 'new_reliability_issues', 'new_security_issues'");
  }

  @Test
  void getNewMetricsInSonarQube108_shouldReturnExactString() {
    String actual = MeasuresWsModule.getNewMetricsInSonarQube108();
    assertThat(actual).isEqualTo("'software_quality_blocker_issues', 'software_quality_high_issues', 'software_quality_info_issues', " +
      "'software_quality_medium_issues', 'software_quality_low_issues', 'software_quality_maintainability_issues', " +
      "'software_quality_reliability_issues', 'software_quality_security_issues', " +
      "'new_software_quality_blocker_issues', 'new_software_quality_high_issues', 'new_software_quality_info_issues', " +
      "'new_software_quality_medium_issues', 'new_software_quality_low_issues', 'new_software_quality_maintainability_issues', " +
      "'new_software_quality_reliability_issues', 'new_software_quality_security_issues'");
  }

  @Test
  void getUndeprecatedMetricsinSonarQube108_shouldReturnExactString() {
    String actual = MeasuresWsModule.getUndeprecatedMetricsinSonarQube108();
    assertThat(actual).isEqualTo("'bugs', 'new_bugs', 'vulnerabilities', 'new_vulnerabilities', 'code_smells', 'new_code_smells', 'high_impact_accepted_issues', " +
      "'new_blocker_violations', 'new_critical_violations', 'new_major_violations', 'new_minor_violations', 'new_info_violations', 'blocker_violations', " +
      "'critical_violations', 'major_violations', 'minor_violations', 'info_violations'");
  }
}
