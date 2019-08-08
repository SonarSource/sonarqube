/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateConditionsUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.CreateConditionResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ERROR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_METRIC;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_OPERATOR;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ORGANIZATION;

@RunWith(DataProviderRunner.class)
public class CreateConditionActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private CreateConditionAction underTest = new CreateConditionAction(dbClient, new QualityGateConditionsUpdater(dbClient),
    new QualityGatesWsSupport(dbClient, userSession, defaultOrganizationProvider));

  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void create_error_condition() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQualityGateAdmin(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = insertMetric();

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertCondition(qualityGate, metric, "LT", "90");
  }

  @Test
  public void create_condition_over_leak_period() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQualityGateAdmin(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = insertMetric();

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertCondition(qualityGate, metric, "LT", "90");
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    logInAsQualityGateAdmin(db.getDefaultOrganization());
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto otherQualityGate = db.qualityGates().insertQualityGate(otherOrganization);

    MetricDto metric = insertMetric();

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "10")
      .execute();

    assertCondition(qualityGate, metric, "LT", "10");
  }

  @Test
  public void fail_to_update_built_in_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQualityGateAdmin(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setBuiltIn(true));
    MetricDto metric = insertMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_with_unknown_operator() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQualityGateAdmin(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false).setDirection(0));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'op' (ABC) must be one of: [LT, GT]");

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "ABC")
      .setParam(PARAM_ERROR, "90")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  @UseDataProvider("invalid_operators_for_direction")
  public void fail_with_invalid_operators_for_direction(String operator, int direction) {
    OrganizationDto organization = db.organizations().insert();
    logInAsQualityGateAdmin(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(INT.name()).setHidden(false).setDirection(direction));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Operator %s is not allowed for this metric.", operator));

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, operator)
      .setParam(PARAM_ERROR, "90")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void test_response() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQualityGateAdmin(organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = insertMetric();

    CreateConditionResponse response = ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "45")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(CreateConditionResponse.class);

    QualityGateConditionDto condition = new ArrayList<>(dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getId())).get(0);
    assertThat(response.getId()).isEqualTo(condition.getId());
    assertThat(response.getMetric()).isEqualTo(metric.getKey());
    assertThat(response.getOp()).isEqualTo("LT");
    assertThat(response.getError()).isEqualTo("45");
  }

  @Test
  public void throw_ForbiddenException_if_not_gate_administrator() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    MetricDto metric = insertMetric();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    ws.newRequest()
      .setParam(PARAM_GATE_ID, qualityGate.getId().toString())
      .setParam(PARAM_METRIC, metric.getKey())
      .setParam(PARAM_OPERATOR, "LT")
      .setParam(PARAM_ERROR, "90")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
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
        tuple("gateId", true),
        tuple("metric", true),
        tuple("error", true),
        tuple("op", false),
        tuple("organization", false));
  }

  @DataProvider
  public static Object[][] invalid_operators_for_direction() {
    return new Object[][] {
      {"LT", -1},
      {"GT", 1},
    };
  }

  private void assertCondition(QualityGateDto qualityGate, MetricDto metric, String operator, String error) {
    assertThat(dbClient.gateConditionDao().selectForQualityGate(dbSession, qualityGate.getId()))
      .extracting(QualityGateConditionDto::getQualityGateId, QualityGateConditionDto::getMetricId, QualityGateConditionDto::getOperator,
        QualityGateConditionDto::getErrorThreshold)
      .containsExactlyInAnyOrder(tuple(qualityGate.getId(), metric.getId().longValue(), operator, error));
  }

  private void logInAsQualityGateAdmin(OrganizationDto organization) {
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, organization);
  }

  private MetricDto insertMetric() {
    return db.measures().insertMetric(m -> m
      .setValueType(INT.name())
      .setHidden(false)
      .setDirection(0));
  }
}
