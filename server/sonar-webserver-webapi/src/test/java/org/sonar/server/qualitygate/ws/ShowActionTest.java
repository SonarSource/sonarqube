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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
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
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
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
    new ShowAction(db.getDbClient(), new QualityGateFinder(db.getDbClient()),
      new QualityGatesWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider)));

  @Test
  public void show() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);
    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();
    QualityGateConditionDto condition1 = db.qualityGates().addCondition(qualityGate, metric1, c -> c.setOperator("GT"));
    QualityGateConditionDto condition2 = db.qualityGates().addCondition(qualityGate, metric2, c -> c.setOperator("LT"));

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getId()).isEqualTo(qualityGate.getId());
    assertThat(response.getName()).isEqualTo(qualityGate.getName());
    assertThat(response.getIsBuiltIn()).isFalse();
    assertThat(response.getConditionsList()).hasSize(2);
    assertThat(response.getConditionsList())
      .extracting(Condition::getId, Condition::getMetric, Condition::getOp, Condition::getError)
      .containsExactlyInAnyOrder(
        tuple(condition1.getId(), metric1.getKey(), "GT", condition1.getErrorThreshold()),
        tuple(condition2.getId(), metric2.getKey(), "LT", condition2.getErrorThreshold()));
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto otherQualityGate = db.qualityGates().insertQualityGate(otherOrganization);
    db.qualityGates().setDefaultQualityGate(db.getDefaultOrganization(), qualityGate);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getId()).isEqualTo(qualityGate.getId());
  }

  @Test
  public void show_built_in() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setBuiltIn(true));
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getIsBuiltIn()).isTrue();
  }

  @Test
  public void show_by_id() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);

    ShowWsResponse response = ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getId()).isEqualTo(qualityGate.getId());
    assertThat(response.getName()).isEqualTo(qualityGate.getName());
  }

  @Test
  public void no_condition() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getId()).isEqualTo(qualityGate.getId());
    assertThat(response.getName()).isEqualTo(qualityGate.getName());
    assertThat(response.getConditionsList()).isEmpty();
  }

  @Test
  public void actions() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    QGateWithOrgDto qualityGate2 = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate2);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getRename()).isTrue();
    assertThat(actions.getManageConditions()).isTrue();
    assertThat(actions.getDelete()).isTrue();
    assertThat(actions.getCopy()).isTrue();
    assertThat(actions.getSetAsDefault()).isTrue();
    assertThat(actions.getAssociateProjects()).isTrue();
  }

  @Test
  public void actions_on_default() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getRename()).isTrue();
    assertThat(actions.getManageConditions()).isTrue();
    assertThat(actions.getDelete()).isFalse();
    assertThat(actions.getCopy()).isTrue();
    assertThat(actions.getSetAsDefault()).isFalse();
    assertThat(actions.getAssociateProjects()).isFalse();
  }

  @Test
  public void actions_on_built_in() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setBuiltIn(true));
    QGateWithOrgDto qualityGate2 = db.qualityGates().insertQualityGate(organization, qg -> qg.setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(organization, qualityGate2);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getRename()).isFalse();
    assertThat(actions.getManageConditions()).isFalse();
    assertThat(actions.getDelete()).isFalse();
    assertThat(actions.getCopy()).isTrue();
    assertThat(actions.getSetAsDefault()).isTrue();
    assertThat(actions.getAssociateProjects()).isTrue();
  }

  @Test
  public void actions_when_not_quality_gate_administer() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setBuiltIn(true));
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    Actions actions = response.getActions();
    assertThat(actions.getRename()).isFalse();
    assertThat(actions.getManageConditions()).isFalse();
    assertThat(actions.getDelete()).isFalse();
    assertThat(actions.getCopy()).isFalse();
    assertThat(actions.getSetAsDefault()).isFalse();
    assertThat(actions.getAssociateProjects()).isFalse();
  }

  @Test
  public void show_on_paid_organization() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);
    MetricDto metric = db.measures().insertMetric();
    db.qualityGates().addCondition(qualityGate, metric);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addMembership(organization);

    ShowWsResponse response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getConditionsList()).hasSize(1);
  }

  @Test
  public void fail_when_no_name_or_id() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'name' must be provided");

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_both_name_or_id() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'name' must be provided");

    ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("id", qualityGate.getId().toString())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_condition_is_on_disabled_metric() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);
    MetricDto metric = db.measures().insertMetric();
    db.qualityGates().addCondition(qualityGate, metric);
    db.getDbClient().metricDao().disableCustomByKey(db.getSession(), metric.getKey());
    db.commit();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Could not find metric with id %s", metric.getId()));

    ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_quality_name_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No quality gate has been found for name UNKNOWN");

    ws.newRequest()
      .setParam("name", "UNKNOWN")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_quality_id_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No quality gate has been found for id 123");

    ws.newRequest()
      .setParam("id", "123")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_organization_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'Unknown'");

    ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", "Unknown")
      .execute();
  }

  @Test
  public void fail_when_quality_gate_belongs_to_another_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(otherOrganization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("No quality gate has been found for name %s", qualityGate.getName()));

    ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_quality_gate_belongs_to_another_organization_using_id_parameter() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(otherOrganization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("No quality gate has been found for id %s in organization %s", qualityGate.getId(), organization.getName()));

    ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_on_paid_organization_when_not_member() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage(format("You're not member of organization '%s'", organization.getKey()));

    ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("admin").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("My Quality Gate"));
    QGateWithOrgDto qualityGate2 = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("My Quality Gate 2"));
    db.qualityGates().setDefaultQualityGate(organization, qualityGate2);
    MetricDto blockerViolationsMetric = db.measures().insertMetric(m -> m.setKey("blocker_violations"));
    MetricDto criticalViolationsMetric = db.measures().insertMetric(m -> m.setKey("tests"));
    db.qualityGates().addCondition(qualityGate, blockerViolationsMetric, c -> c.setOperator("GT").setErrorThreshold("0"));
    db.qualityGates().addCondition(qualityGate, criticalViolationsMetric, c -> c.setOperator("LT").setErrorThreshold("10"));

    String response = ws.newRequest()
      .setParam("name", qualityGate.getName())
      .setParam("organization", organization.getKey())
      .execute()
      .getInput();

    assertJson(response).ignoreFields("id")
      .isSimilarTo(getClass().getResource("show-example.json"));
  }

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.params())
      .extracting(Param::key, Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", false),
        tuple("name", false),
        tuple("organization", false));
  }

}
