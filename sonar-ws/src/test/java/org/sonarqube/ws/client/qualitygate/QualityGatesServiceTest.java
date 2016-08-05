
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
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_GATE_ID;

public class QualityGatesServiceTest {
  private static final String PROJECT_ID_VALUE = "195";
  private static final String PROJECT_KEY_VALUE = "project_key_value";
  private static final Long GATE_ID_VALUE = 243L;

  @Rule
  public ServiceTester<QualityGatesService> serviceTester = new ServiceTester<>(new QualityGatesService(mock(WsConnector.class)));

  private QualityGatesService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void associate_project_does_POST_request() {
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
}
