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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.CreateConditionResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;

@RunWith(DataProviderRunner.class)
public class CreateConditionActionTest {


  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final CreateConditionAction underTest = new CreateConditionAction(dbClient, new QualityGateConditionsUpdater(dbClient),
    new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db)));

  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void create_error_condition() {
    logInAsQualityGateAdmin();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute();

    assertCondition(qualityGate, metric, "LT", "90");
  }

  @Test
  public void create_condition_over_new_code_period() {
    logInAsQualityGateAdmin();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute();

    assertCondition(qualityGate, metric, "LT", "90");
  }

  @Test
  public void fail_to_update_built_in_quality_gate() {
    logInAsQualityGateAdmin();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));
    MetricDto metric = insertMetric();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));
  }

  @Test
  public void fail_with_unknown_operator() {
    logInAsQualityGateAdmin();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false).setDirection(0));

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "ABC")
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Value of parameter 'op' (ABC) must be one of: [LT, GT]");
  }

  @Test
  @UseDataProvider("invalid_operators_for_direction")
  public void fail_with_invalid_operators_for_direction(String operator, int direction) {
    logInAsQualityGateAdmin();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false).setDirection(direction));

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, operator)
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Operator %s is not allowed for this metric.", operator));
  }

  @Test
  public void test_response() {
    logInAsQualityGateAdmin();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();

    CreateConditionResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "45")
      .executeProtobuf(CreateConditionResponse.class);

    QualityGateConditionDto condition = new ArrayList<>(dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getUuid())).get(0);
    assertThat(response.getId()).isEqualTo(condition.getUuid());
    assertThat(response.getMetric()).isEqualTo(metric.getKey());
    assertThat(response.getOp()).isEqualTo("LT");
    assertThat(response.getError()).isEqualTo("45");
  }

  @Test
  public void user_with_permission_can_call_endpoint() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    userSession.logIn(user);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void user_with_group_permission_can_call_endpoint() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(qualityGate, group);
    userSession.logIn(user).setGroups(group);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void throw_ForbiddenException_if_not_gate_administrator() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    userSession.logIn();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("gateId", false),
        tuple("gateName", false),
        tuple("metric", true),
        tuple("error", true),
        tuple("op", false));
  }

  @DataProvider
  public static Object[][] invalid_operators_for_direction() {
    return new Object[][] {
      {"LT", -1},
      {"GT", 1},
    };
  }

  private void assertCondition(QualityGateDto qualityGate, MetricDto metric, String operator, String error) {
    assertThat(dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getUuid()))
      .extracting(QualityGateConditionDto::getQualityGateUuid, QualityGateConditionDto::getMetricUuid, QualityGateConditionDto::getOperator,
        QualityGateConditionDto::getErrorThreshold)
      .containsExactlyInAnyOrder(tuple(qualityGate.getUuid(), metric.getUuid(), operator, error));
  }

  private void logInAsQualityGateAdmin() {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);
  }

  private MetricDto insertMetric() {
    return db.measures().insertMetric(m -> m
      .setValueType(INT.name())
      .setHidden(false)
      .setDirection(0));
  }
}
