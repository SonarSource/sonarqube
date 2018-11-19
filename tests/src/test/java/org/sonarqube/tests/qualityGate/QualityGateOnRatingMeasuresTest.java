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
package org.sonarqube.tests.qualityGate;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category1Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.WsQualityGates;
import org.sonarqube.ws.client.qualitygate.CreateConditionRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasure;
import static util.ItUtils.runProjectAnalysis;

public class QualityGateOnRatingMeasuresTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void generate_warning_qgate_on_rating_metric() throws Exception {
    Project project = tester.projects().generate(null);
    WsQualityGates.CreateWsResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.qGates().service().createCondition(CreateConditionRequest.builder()
      .setQualityGateId(qualityGate.getId())
      .setMetricKey("security_rating")
      .setOperator("GT")
      .setWarning("3")
      .build());
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityGate/QualityGateOnRatingMeasuresTest/with-many-rules.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(project.getKey(), "xoo", "with-many-rules");

    runProjectAnalysis(orchestrator, "qualitygate/xoo-sample", "sonar.projectKey", project.getKey());

    assertThat(getGateStatusMeasure(project).getValue()).isEqualTo("WARN");
  }

  @Test
  public void generate_error_qgate_on_rating_metric_on_leak_period() throws Exception {
    Project project = tester.projects().generate(null);
    WsQualityGates.CreateWsResponse qualityGate = tester.qGates().generate();
    tester.qGates().associateProject(qualityGate, project);
    tester.settings().setGlobalSetting("sonar.leak.period", "previous_version");
    tester.wsClient().qualityGates().createCondition(CreateConditionRequest.builder()
      .setQualityGateId(qualityGate.getId())
      .setMetricKey("new_security_rating")
      .setOperator("GT")
      .setError("3")
      .setPeriod(1)
      .build());

    // Run first analysis with empty quality gate -> quality gate is green
    orchestrator.getServer().associateProjectToQualityProfile(project.getKey(), "xoo", "empty");
    runProjectAnalysis(orchestrator, "qualitygate/xoo-sample", "sonar.projectKey", project.getKey());
    assertThat(getGateStatusMeasure(project).getValue()).isEqualTo("OK");

    // Run second analysis with some rules that makes Security Rating to E -> quality gate is red
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityGate/QualityGateOnRatingMeasuresTest/with-many-rules.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(project.getKey(), "xoo", "with-many-rules");
    runProjectAnalysis(orchestrator, "qualitygate/xoo-sample", "sonar.projectKey", project.getKey());
    assertThat(getGateStatusMeasure(project).getValue()).isEqualTo("ERROR");
  }

  private WsMeasures.Measure getGateStatusMeasure(Project project) {
    return getMeasure(orchestrator, project.getKey(), "alert_status");
  }

}
