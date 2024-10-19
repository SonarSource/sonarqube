/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.db.component.SnapshotDto;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse.ProjectStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;

public class QualityGateDetailsFormatterTest {

  private QualityGateDetailsFormatter underTest;

  @Test
  public void map_level_conditions_and_period() throws IOException {
    String measureData = IOUtils.toString(getClass().getResource("QualityGateDetailsFormatterTest/quality_gate_details.json"));
    SnapshotDto snapshot = new SnapshotDto()
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(1449404331764L);
    underTest = newQualityGateDetailsFormatter(measureData, snapshot);

    ProjectStatus result = underTest.format();

    assertThat(result.getStatus()).isEqualTo(ProjectStatusResponse.Status.ERROR);
    assertEquals(NON_COMPLIANT.toString(), result.getCaycStatus());
    // check conditions
    assertThat(result.getConditionsCount()).isEqualTo(3);
    List<ProjectStatusResponse.Condition> conditions = result.getConditionsList();
    assertThat(conditions).extracting("status").containsExactly(
      ProjectStatusResponse.Status.ERROR,
      ProjectStatusResponse.Status.WARN,
      ProjectStatusResponse.Status.OK);
    assertThat(conditions).extracting("metricKey").containsExactly("new_coverage", "new_blocker_violations", "new_critical_violations");
    assertThat(conditions).extracting("comparator").containsExactly(
      ProjectStatusResponse.Comparator.LT,
      ProjectStatusResponse.Comparator.GT,
      ProjectStatusResponse.Comparator.GT);
    assertThat(conditions).extracting("warningThreshold").containsOnly("80", "");
    assertThat(conditions).extracting("errorThreshold").containsOnly("85", "0", "0");
    assertThat(conditions).extracting("actualValue").containsExactly("82.2985024398452", "1", "0");

    // check period
    ProjectStatusResponse.NewCodePeriod period = result.getPeriod();
    assertThat(period).extracting("mode").isEqualTo("last_version");
    assertThat(period).extracting("parameter").isEqualTo("2015-12-07");
    assertThat(period.getDate()).isEqualTo(formatDateTime(snapshot.getPeriodDate()));
  }

  @Test
  public void ignore_period_not_set_on_leak_period() throws IOException {
    String measureData = IOUtils.toString(getClass().getResource("QualityGateDetailsFormatterTest/non_leak_period.json"));
    SnapshotDto snapshot = new SnapshotDto()
      .setPeriodMode("last_version")
      .setPeriodParam("2015-12-07")
      .setPeriodDate(1449404331764L);
    underTest = newQualityGateDetailsFormatter(measureData, snapshot);

    ProjectStatus result = underTest.format();

    // check conditions
    assertThat(result.getConditionsCount()).isOne();
    List<ProjectStatusResponse.Condition> conditions = result.getConditionsList();
    assertThat(conditions).extracting("status").containsExactly(ProjectStatusResponse.Status.ERROR);
    assertThat(conditions).extracting("metricKey").containsExactly("new_coverage");
    assertThat(conditions).extracting("comparator").containsExactly(ProjectStatusResponse.Comparator.LT);
    assertThat(conditions).extracting("errorThreshold").containsOnly("85");
    assertThat(conditions).extracting("actualValue").containsExactly("82.2985024398452");
  }

  @Test
  public void fail_when_measure_level_is_unknown() {
    String measureData = "{\n" +
      "  \"level\": \"UNKNOWN\",\n" +
      "  \"conditions\": [\n" +
      "    {\n" +
      "      \"metric\": \"new_coverage\",\n" +
      "      \"op\": \"LT\",\n" +
      "      \"period\": 1,\n" +
      "      \"warning\": \"80\",\n" +
      "      \"error\": \"85\",\n" +
      "      \"actual\": \"82.2985024398452\",\n" +
      "      \"level\": \"ERROR\"\n" +
      "    }\n" +
      "  ]\n" +
      "}";
    underTest = newQualityGateDetailsFormatter(measureData, new SnapshotDto());

    assertThatThrownBy(() -> underTest.format())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Unknown quality gate status 'UNKNOWN'");
  }

  @Test
  public void fail_when_measure_op_is_unknown() {
    String measureData = "{\n" +
      "  \"level\": \"ERROR\",\n" +
      "  \"conditions\": [\n" +
      "    {\n" +
      "      \"metric\": \"new_coverage\",\n" +
      "      \"op\": \"UNKNOWN\",\n" +
      "      \"period\": 1,\n" +
      "      \"warning\": \"80\",\n" +
      "      \"error\": \"85\",\n" +
      "      \"actual\": \"82.2985024398452\",\n" +
      "      \"level\": \"ERROR\"\n" +
      "    }\n" +
      "  ]\n" +
      "}";
    underTest = newQualityGateDetailsFormatter(measureData, new SnapshotDto());

    assertThatThrownBy(() -> underTest.format())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Unknown quality gate comparator 'UNKNOWN'");
  }

  private static QualityGateDetailsFormatter newQualityGateDetailsFormatter(@Nullable String measureData, @Nullable SnapshotDto snapshotDto) {
    return new QualityGateDetailsFormatter(measureData, snapshotDto, NON_COMPLIANT);
  }
}
