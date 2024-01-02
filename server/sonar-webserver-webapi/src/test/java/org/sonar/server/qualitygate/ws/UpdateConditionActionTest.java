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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
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
import org.sonar.server.exceptions.NotFoundException;
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
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;

@RunWith(DataProviderRunner.class)
public class UpdateConditionActionTest {


  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final UpdateConditionAction underTest = new UpdateConditionAction(dbClient, new QualityGateConditionsUpdater(dbClient),
    new QualityGatesWsSupport(dbClient, userSession, TestComponentFinder.from(db)));

  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void update_error_condition() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("GT").setErrorThreshold("80"));

    ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute();

    assertCondition(qualityGate, metric, "LT", "90");
  }

  @Test
  public void test_response() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("GT").setErrorThreshold("80"));

    CreateConditionResponse response = ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "45")
      .executeProtobuf(CreateConditionResponse.class);

    assertThat(response.getId()).isEqualTo(condition.getUuid());
    assertThat(response.getMetric()).isEqualTo(metric.getKey());
    assertThat(response.getOp()).isEqualTo("LT");
    assertThat(response.getError()).isEqualTo("45");
  }

  @Test
  public void user_with_permission_can_call_endpoint() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    userSession.logIn(user);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "45")
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void user_with_group_permission_can_call_endpoint() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.qualityGates().addGroupPermission(qualityGate, group);
    userSession.logIn(user).setGroups(group);

    TestResponse response = ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "45")
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void fail_to_update_built_in_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));
    MetricDto metric = insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "10")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));
  }

  @Test
  public void fail_on_unknown_condition() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    db.qualityGates().addCondition(qualityGate, metric);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, "123")
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("No quality gate condition with uuid '123'");
  }

  @Test
  public void fail_when_condition_match_unknown_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    MetricDto metric = insertMetric();
    QualityGateConditionDto condition = new QualityGateConditionDto().setUuid("uuid")
      .setMetricUuid("metric")
      .setQualityGateUuid("123");
    db.getDbClient().gateConditionDao().insert(condition, dbSession);
    db.commit();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(format("Condition '%s' is linked to an unknown quality gate '%s'", condition.getUuid(), 123L));
  }

  @Test
  public void fail_with_unknown_operator() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false).setDirection(0));
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator("LT").setErrorThreshold("80"));

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "ABC")
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Value of parameter 'op' (ABC) must be one of: [LT, GT]");
  }

  @Test
  @UseDataProvider("update_invalid_operators_and_direction")
  public void fail_with_invalid_operators_for_direction(String validOperator, String updateOperator, int direction) {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false).setDirection(direction));
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric,
      c -> c.setOperator(validOperator).setErrorThreshold("80"));

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, updateOperator)
      .setParam(PARAM_ERROR, "90")
      .execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining(format("Operator %s is not allowed for this metric.", updateOperator));
  }

  @Test
  public void throw_ForbiddenException_if_not_gate_administrator() {
    userSession.logIn();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = insertMetric();
    QualityGateConditionDto condition = db.qualityGates().addCondition(qualityGate, metric);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_ID, condition.getUuid())
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
    assertThat(action.responseExampleAsString()).isNull();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", true),
        tuple("metric", true),
        tuple("error", true),
        tuple("op", false));
  }

  @DataProvider
  public static Object[][] update_invalid_operators_and_direction() {
    return new Object[][]{
      {"GT", "LT", -1},
      {"LT", "GT", 1},
    };
  }

  private void assertCondition(QualityGateDto qualityGate, MetricDto metric, String operator, String error) {
    assertThat(dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getUuid()))
      .extracting(QualityGateConditionDto::getQualityGateUuid, QualityGateConditionDto::getMetricUuid, QualityGateConditionDto::getOperator,
        QualityGateConditionDto::getErrorThreshold)
      .containsExactlyInAnyOrder(tuple(qualityGate.getUuid(), metric.getUuid(), operator, error));
  }

  private MetricDto insertMetric() {
    return db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false).setDirection(0));
  }
}
