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

package org.sonar.server.qualitygate.ws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsQualityGates.CreateConditionWsResponse;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_GATE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.server.computation.task.projectanalysis.metric.Metric.MetricType.PERCENT;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PERIOD;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_WARNING;

public class UpdateConditionActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();
  QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);

  UpdateConditionAction underTest = new UpdateConditionAction(userSession, dbClient, new QualityGateConditionsUpdater(dbClient));

  QualityGateDto qualityGateDto;
  QualityGateConditionDto conditionDto;

  MetricDto coverageMetricDto = newMetricDto()
    .setKey("coverage")
    .setShortName("Coverage")
    .setValueType(PERCENT.name())
    .setHidden(false);

  WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() throws Exception {
    MetricDto metricDto = dbClient.metricDao().insert(dbSession, coverageMetricDto);
    qualityGateDto = qualityGateDbTester.insertQualityGate();
    conditionDto = new QualityGateConditionDto().setQualityGateId(qualityGateDto.getId())
      .setMetricId(metricDto.getId())
      .setOperator("GT")
      .setWarningThreshold(null)
      .setErrorThreshold("80")
      .setPeriod(1);
    dbClient.gateConditionDao().insert(conditionDto, dbSession);
    dbSession.commit();
  }

  @Test
  public void update_warning_condition() throws Exception {
    setUserAsQualityGateAdmin();

    CreateConditionWsResponse response = executeRequest(conditionDto.getId(), coverageMetricDto.getKey(), "LT", "90", null, null);

    assertCondition(response, "LT", "90", null, null);
  }

  @Test
  public void update_error_condition() throws Exception {
    setUserAsQualityGateAdmin();

    CreateConditionWsResponse response = executeRequest(conditionDto.getId(), coverageMetricDto.getKey(), "LT", null, "90", null);

    assertCondition(response, "LT", null, "90", null);
  }

  @Test
  public void update_condition_over_leak_period() throws Exception {
    setUserAsQualityGateAdmin();

    CreateConditionWsResponse response = executeRequest(conditionDto.getId(), coverageMetricDto.getKey(), "LT", null, "90", 1);

    assertCondition(response, "LT", null, "90", 1);
  }

  @Test
  public void fail_when_not_quality_gate_admin() throws Exception {
    setUserAsNotQualityGateAdmin();

    expectedException.expect(ForbiddenException.class);
    executeRequest(conditionDto.getId(), coverageMetricDto.getKey(), "LT", "90", null, null);
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNull();
    assertThat(action.params()).hasSize(6);
  }

  private void assertCondition(CreateConditionWsResponse response, String operator, @Nullable String warning, @Nullable String error, @Nullable Integer period) {
    List<QualityGateConditionDto> conditionDtoList = new ArrayList<>(dbClient.gateConditionDao().selectForQualityGate(qualityGateDto.getId(), dbSession));
    assertThat(conditionDtoList).hasSize(1);
    QualityGateConditionDto qualityGateConditionDto = conditionDtoList.get(0);
    assertThat(qualityGateConditionDto.getQualityGateId()).isEqualTo(qualityGateDto.getId());
    assertThat(qualityGateConditionDto.getMetricId()).isEqualTo(coverageMetricDto.getId().longValue());
    assertThat(qualityGateConditionDto.getOperator()).isEqualTo(operator);
    assertThat(qualityGateConditionDto.getWarningThreshold()).isEqualTo(warning);
    assertThat(qualityGateConditionDto.getErrorThreshold()).isEqualTo(error);
    assertThat(qualityGateConditionDto.getPeriod()).isEqualTo(period);

    assertThat(response.getId()).isEqualTo(qualityGateConditionDto.getId());
    assertThat(response.getMetric()).isEqualTo(coverageMetricDto.getKey());
    assertThat(response.getOp()).isEqualTo(operator);
    if (warning != null) {
      assertThat(response.getWarning()).isEqualTo(warning);
    } else {
      assertThat(response.hasWarning()).isFalse();
    }
    if (error != null) {
      assertThat(response.getError()).isEqualTo(error);
    } else {
      assertThat(response.hasError()).isFalse();
    }
    if (period != null) {
      assertThat(response.getPeriod()).isEqualTo(period);
    } else {
      assertThat(response.hasPeriod()).isFalse();
    }
  }

  private CreateConditionWsResponse executeRequest(long conditionId, String metricKey, String operator, @Nullable String warning, @Nullable String error,
    @Nullable Integer period) {
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_ID, Long.toString(conditionId))
      .setParam(PARAM_METRIC, metricKey)
      .setParam(PARAM_OPERATOR, operator);
    if (warning != null) {
      request.setParam(PARAM_WARNING, warning);
    }
    if (error != null) {
      request.setParam(PARAM_ERROR, error);
    }
    if (period != null) {
      request.setParam(PARAM_PERIOD, Integer.toString(period));
    }
    try {
      return CreateConditionWsResponse.parseFrom(request.execute().getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void setUserAsQualityGateAdmin() {
    userSession.login("project-admin").setGlobalPermissions(QUALITY_GATE_ADMIN);
  }

  private void setUserAsNotQualityGateAdmin() {
    userSession.login("not-admin").setGlobalPermissions(SCAN_EXECUTION);
  }
}
