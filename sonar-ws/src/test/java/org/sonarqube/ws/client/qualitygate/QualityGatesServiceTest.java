/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsQualityGates.CreateConditionWsResponse;
import org.sonarqube.ws.WsQualityGates.CreateWsResponse;
import org.sonarqube.ws.WsQualityGates.ProjectStatusWsResponse;
import org.sonarqube.ws.WsQualityGates.UpdateConditionWsResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ANALYSIS_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_WARNING;

public class QualityGatesServiceTest {
  private static final String PROJECT_ID_VALUE = "195";
  private static final String PROJECT_KEY_VALUE = "project_key_value";
  private static final Long GATE_ID_VALUE = 243L;

  @Rule
  public ServiceTester<QualityGatesService> serviceTester = new ServiceTester<>(new QualityGatesService(mock(WsConnector.class)));

  private QualityGatesService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void associate_project() {
    underTest.associateProject(new SelectWsRequest()
      .setGateId(GATE_ID_VALUE)
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE));

    assertThat(serviceTester.getPostParser()).isNull();

    PostRequest postRequest = serviceTester.getPostRequest();

    serviceTester.assertThat(postRequest)
      .hasPath("select")
      .hasParam(PARAM_GATE_ID, String.valueOf(GATE_ID_VALUE))
      .hasParam(PARAM_PROJECT_ID, String.valueOf(PROJECT_ID_VALUE))
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void project_status() {
    underTest.projectStatus(new ProjectStatusWsRequest()
      .setAnalysisId("analysisId")
      .setProjectId("projectId")
      .setProjectKey("projectKey"));
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ProjectStatusWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_ANALYSIS_ID, "analysisId")
      .hasParam(PARAM_PROJECT_ID, "projectId")
      .hasParam(PARAM_PROJECT_KEY, "projectKey")
      .andNoOtherParam();
  }

  @Test
  public void create() {
    underTest.create("Default");
    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(CreateWsResponse.parser());
    serviceTester.assertThat(request)
      .hasParam(PARAM_NAME, "Default")
      .andNoOtherParam();
  }

  @Test
  public void create_condition() {
    underTest.createCondition(CreateConditionRequest.builder()
      .setQualityGateId(10)
      .setMetricKey("metric")
      .setOperator("LT")
      .setWarning("warning")
      .setError("error")
      .setPeriod(1)
    .build());

    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(CreateConditionWsResponse.parser());
    serviceTester.assertThat(request)
      .hasPath("create_condition")
      .hasParam(PARAM_GATE_ID, 10)
      .hasParam(PARAM_METRIC, "metric")
      .hasParam(PARAM_OPERATOR, "LT")
      .hasParam(PARAM_WARNING, "warning")
      .hasParam(PARAM_ERROR, "error")
      .hasParam(PARAM_PERIOD, 1)
      .andNoOtherParam();
  }

  @Test
  public void update_condition() {
    underTest.updateCondition(UpdateConditionRequest.builder()
      .setConditionId(10)
      .setMetricKey("metric")
      .setOperator("LT")
      .setWarning("warning")
      .setError("error")
      .setPeriod(1)
      .build());

    PostRequest request = serviceTester.getPostRequest();

    assertThat(serviceTester.getPostParser()).isSameAs(UpdateConditionWsResponse.parser());
    serviceTester.assertThat(request)
      .hasPath("update_condition")
      .hasParam(PARAM_ID, 10)
      .hasParam(PARAM_METRIC, "metric")
      .hasParam(PARAM_OPERATOR, "LT")
      .hasParam(PARAM_WARNING, "warning")
      .hasParam(PARAM_ERROR, "error")
      .hasParam(PARAM_PERIOD, 1)
      .andNoOtherParam();
  }
}
