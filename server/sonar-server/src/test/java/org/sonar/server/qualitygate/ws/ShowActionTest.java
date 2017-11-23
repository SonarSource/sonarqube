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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.ShowWsResponse;
import org.sonarqube.ws.Qualitygates.ShowWsResponse.Condition;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Qualitygates.Actions;

public class ShowActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private WsActionTester ws = new WsActionTester(
    new ShowAction(db.getDbClient(), new QualityGateFinder(db.getDbClient()), new QGateWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider)));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription)
      .containsExactlyInAnyOrder(
        tuple("7.0", "'isBuiltIn' field is added to the response"),
        tuple("7.0", "'actions' field is added in the response"));
    assertThat(action.params())
      .extracting(Param::key, Param::isRequired)
      .containsExactlyInAnyOrder(tuple("id", false), tuple("name", false));
  }

  @Test
  public void json_example() {
    userSession.logIn("admin").addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate("My Quality Gate");
    MetricDto blockerViolationsMetric = db.measures().insertMetric(m -> m.setKey("blocker_violations"));
    MetricDto criticalViolationsMetric = db.measures().insertMetric(m -> m.setKey("critical_violations"));
    db.qualityGates().addCondition(qualityGate, blockerViolationsMetric, c -> c.setOperator("GT").setPeriod(null).setErrorThreshold("0").setWarningThreshold(null));
    db.qualityGates().addCondition(qualityGate, criticalViolationsMetric, c -> c.setOperator("LT").setPeriod(1).setErrorThreshold(null).setWarningThreshold("0"));

    String response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .execute()
      .getInput();

    assertJson(response).ignoreFields("id")
      .isSimilarTo(getClass().getResource("show-example.json"));
  }

  @Test
  public void show() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    QualityGateConditionDto condition1 = db.qualityGates().addCondition(qualityGate, metric, c -> c.setOperator("GT").setPeriod(null));
    QualityGateConditionDto condition2 = db.qualityGates().addCondition(qualityGate, metric, c -> c.setOperator("LT").setPeriod(1));

    ShowWsResponse response = ws.newRequest().setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getId()).isEqualTo(qualityGate.getId());
    assertThat(response.getName()).isEqualTo(qualityGate.getName());
    assertThat(response.getIsBuiltIn()).isFalse();
    assertThat(response.getConditionsList()).hasSize(2);
    assertThat(response.getConditionsList())
      .extracting(Condition::getId, Condition::getMetric, Condition::hasPeriod, Condition::getPeriod, Condition::getOp, Condition::getError, Condition::getWarning)
      .containsExactlyInAnyOrder(
        tuple(condition1.getId(), metric.getKey(), false, 0, "GT", condition1.getErrorThreshold(), condition1.getWarningThreshold()),
        tuple(condition2.getId(), metric.getKey(), true, 1, "LT", condition2.getErrorThreshold(), condition2.getWarningThreshold()));
  }

  @Test
  public void show_built_in() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));

    ShowWsResponse response = ws.newRequest().setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getIsBuiltIn()).isTrue();
  }

  @Test
  public void show_by_id() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    ShowWsResponse response = ws.newRequest().setParam("id", qualityGate.getId().toString())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getId()).isEqualTo(qualityGate.getId());
    assertThat(response.getName()).isEqualTo(qualityGate.getName());
  }

  @Test
  public void no_condition() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    ShowWsResponse response = ws.newRequest().setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getId()).isEqualTo(qualityGate.getId());
    assertThat(response.getName()).isEqualTo(qualityGate.getName());
    assertThat(response.getConditionsList()).isEmpty();
  }

  @Test
  public void actions() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    ShowWsResponse response = ws.newRequest().setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getEdit()).isTrue();
    assertThat(actions.getCopy()).isTrue();
    assertThat(actions.getSetAsDefault()).isTrue();
    assertThat(actions.getAssociateProjects()).isTrue();
  }

  @Test
  public void actions_on_default() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(qualityGate);

    ShowWsResponse response = ws.newRequest().setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getEdit()).isTrue();
    assertThat(actions.getCopy()).isTrue();
    assertThat(actions.getSetAsDefault()).isFalse();
    assertThat(actions.getAssociateProjects()).isFalse();
  }

  @Test
  public void actions_on_built_in() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));

    ShowWsResponse response = ws.newRequest().setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getEdit()).isFalse();
    assertThat(actions.getCopy()).isTrue();
    assertThat(actions.getSetAsDefault()).isTrue();
    assertThat(actions.getAssociateProjects()).isTrue();
  }

  @Test
  public void actions_when_not_quality_gate_administer() {
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setBuiltIn(true));

    ShowWsResponse response = ws.newRequest().setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getEdit()).isFalse();
    assertThat(actions.getCopy()).isFalse();
    assertThat(actions.getSetAsDefault()).isFalse();
    assertThat(actions.getAssociateProjects()).isFalse();
  }

  @Test
  public void fail_when_no_name_or_id() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'name' must be provided");

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_both_name_or_id() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'name' must be provided");

    ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("id", qualityGate.getId().toString())
      .execute();
  }

  @Test
  public void fail_when_condition_is_on_disabled_metric() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    MetricDto metric = db.measures().insertMetric();
    db.qualityGates().addCondition(qualityGate, metric);
    db.getDbClient().metricDao().disableCustomByKey(db.getSession(), metric.getKey());
    db.commit();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Could not find metric with id %s", metric.getId()));

    ws.newRequest()
      .setParam("name", qualityGate.getName())
      .execute();
  }
}
