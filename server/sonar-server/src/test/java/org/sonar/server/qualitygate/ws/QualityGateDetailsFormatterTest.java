/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.SnapshotDto;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse.ProjectStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateDetailsFormatterTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  QualityGateDetailsFormatter underTest;

  @Test
  public void map_level_conditions_and_periods() throws IOException {
    String measureData = IOUtils.toString(getClass().getResource("QualityGateDetailsFormatterTest/quality_gate_details.json"));
    SnapshotDto snapshot = new SnapshotDto()
      .setPeriodMode(1, "last_period")
      .setPeriodDate(1, 1449490731764L)
      .setPeriodMode(2, "last_version")
      .setPeriodParam(2, "2015-12-07")
      .setPeriodDate(2, 1449404331764L)
      .setPeriodMode(3, "last_analysis")
      .setPeriodMode(5, "last_30_days")
      .setPeriodParam(5, "2015-11-07");
    underTest = new QualityGateDetailsFormatter(measureData, snapshot);

    ProjectStatus result = underTest.format();

    assertThat(result.getStatus()).isEqualTo(ProjectStatusWsResponse.Status.ERROR);
    // check conditions
    assertThat(result.getConditionsCount()).isEqualTo(5);
    List<ProjectStatusWsResponse.Condition> conditions = result.getConditionsList();
    assertThat(conditions).extracting("status").containsExactly(
      ProjectStatusWsResponse.Status.ERROR,
      ProjectStatusWsResponse.Status.ERROR,
      ProjectStatusWsResponse.Status.OK,
      ProjectStatusWsResponse.Status.OK,
      ProjectStatusWsResponse.Status.WARN);
    assertThat(conditions).extracting("metricKey").containsExactly("new_coverage", "new_blocker_violations", "new_critical_violations", "new_sqale_debt_ratio",
      "new_sqale_debt_ratio");
    assertThat(conditions).extracting("comparator").containsExactly(
      ProjectStatusWsResponse.Comparator.LT,
      ProjectStatusWsResponse.Comparator.GT,
      ProjectStatusWsResponse.Comparator.NE,
      ProjectStatusWsResponse.Comparator.EQ,
      ProjectStatusWsResponse.Comparator.LT);
    assertThat(conditions).extracting("periodIndex").containsExactly(1, 2, 3, 4, 5);
    assertThat(conditions).extracting("warningThreshold").containsOnly("80", "");
    assertThat(conditions).extracting("errorThreshold").containsOnly("85", "0", "5");
    assertThat(conditions).extracting("actualValue").containsExactly("82.2985024398452", "1", "0", "0.5670339761248853", "0.5670339761248853");

    // check periods
    assertThat(result.getPeriodsCount()).isEqualTo(4);
    List<ProjectStatusWsResponse.Period> periods = result.getPeriodsList();
    assertThat(periods).extracting("index").containsExactly(1, 2, 3, 5);
    assertThat(periods).extracting("mode").containsExactly("last_period", "last_version", "last_analysis", "last_30_days");
    assertThat(periods).extracting("parameter").containsExactly("", "2015-12-07", "", "2015-11-07");
    System.out.println(System.currentTimeMillis());
    assertThat(periods.get(0).getDate()).startsWith("2015-12-07T");
    assertThat(periods.get(1).getDate()).startsWith("2015-12-06T");
  }

  @Test
  public void undefined_quality_gate_when_ill_formatted_measure_data() {
    underTest = new QualityGateDetailsFormatter("", new SnapshotDto());

    ProjectStatus result = underTest.format();

    assertThat(result.getStatus()).isEqualTo(ProjectStatusWsResponse.Status.NONE);
    assertThat(result.getPeriodsCount()).isEqualTo(0);
    assertThat(result.getConditionsCount()).isEqualTo(0);
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
    underTest = new QualityGateDetailsFormatter(measureData, new SnapshotDto());
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unknown quality gate status 'UNKNOWN'");

    underTest.format();
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
    underTest = new QualityGateDetailsFormatter(measureData, new SnapshotDto());
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unknown quality gate comparator 'UNKNOWN'");

    underTest.format();
  }
}
