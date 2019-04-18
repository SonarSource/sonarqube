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
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.ListWsResponse.QualityGate;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Qualitygates.ListWsResponse;

public class ListActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QualityGateFinder qualityGateFinder = new QualityGateFinder(dbClient);

  private WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(),
    new QualityGatesWsSupport(dbClient, userSession, defaultOrganizationProvider), qualityGateFinder));

  @Test
  public void list_quality_gates() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(organization);
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, defaultQualityGate);

    ListWsResponse response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getId, QualityGate::getName, QualityGate::getIsDefault)
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getId(), defaultQualityGate.getName(), true),
        tuple(otherQualityGate.getId(), otherQualityGate.getName(), false));
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    db.qualityGates().setDefaultQualityGate(db.getDefaultOrganization(), qualityGate);

    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto otherQualityGate = db.qualityGates().insertQualityGate(otherOrganization);
    db.qualityGates().setDefaultQualityGate(otherOrganization, otherQualityGate);

    ListWsResponse response = ws.newRequest().executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getId)
      .containsExactlyInAnyOrder(qualityGate.getId());
  }

  @Test
  public void test_built_in_flag() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate(organization, qualityGate -> qualityGate.setBuiltIn(true));
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate(organization, qualityGate -> qualityGate.setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(organization, qualityGate1);

    ListWsResponse response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getId, QualityGate::getIsBuiltIn)
      .containsExactlyInAnyOrder(
        tuple(qualityGate1.getId(), true),
        tuple(qualityGate2.getId(), false));
  }

  @Test
  public void test_deprecated_default_field() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, defaultQualityGate);

    ListWsResponse response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getDefault()).isEqualTo(defaultQualityGate.getId());
  }

  @Test
  public void no_default_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalStateException.class);

    ListWsResponse response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(ListWsResponse.class);

  }

  @Test
  public void actions_with_quality_gate_administer_permission() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Default").setBuiltIn(false));
    QualityGateDto builtInQualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(organization, defaultQualityGate);

    ListWsResponse response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(true);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), true, false, true, true, false, false),
        tuple(builtInQualityGate.getName(), false, false, false, true, true, true),
        tuple(otherQualityGate.getName(), true, true, true, true, true, true));
  }

  @Test
  public void actions_without_quality_gate_administer_permission() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES, organization);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Sonar way").setBuiltIn(true));
    QualityGateDto otherQualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(organization, defaultQualityGate);

    ListWsResponse response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getActions())
      .extracting(ListWsResponse.RootActions::getCreate)
      .isEqualTo(false);
    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName,
        qg -> qg.getActions().getRename(), qg -> qg.getActions().getDelete(), qg -> qg.getActions().getManageConditions(),
        qp -> qp.getActions().getCopy(), qp -> qp.getActions().getSetAsDefault(), qp -> qp.getActions().getAssociateProjects())
      .containsExactlyInAnyOrder(
        tuple(defaultQualityGate.getName(), false, false, false, false, false, false),
        tuple(otherQualityGate.getName(), false, false, false, false, false, false));
  }

  @Test
  public void list_quality_gates_on_paid_organization() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.qualityGates().setDefaultQualityGate(organization, qualityGate);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addMembership(organization);

    ListWsResponse response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .executeProtobuf(ListWsResponse.class);

    assertThat(response.getQualitygatesList())
      .extracting(QualityGate::getName)
      .containsExactlyInAnyOrder(qualityGate.getName());
  }

  @Test
  public void fail_on_paid_organization_when_not_member() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage(format("You're not member of organization '%s'", organization.getKey()));

    ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("admin").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QualityGateDto defaultQualityGate = db.qualityGates().insertQualityGate(organization, qualityGate -> qualityGate.setName("Sonar way").setBuiltIn(true));
    db.qualityGates().insertQualityGate(organization, qualityGate -> qualityGate.setName("Sonar way - Without Coverage").setBuiltIn(false));
    db.qualityGates().setDefaultQualityGate(organization, defaultQualityGate);

    String response = ws.newRequest()
      .setParam("organization", organization.getKey())
      .execute()
      .getInput();

    assertJson(response).ignoreFields("id", "default")
      .isSimilarTo(getClass().getResource("list-example.json"));
  }

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.key()).isEqualTo("list");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription)
      .containsExactlyInAnyOrder(
        tuple("7.0", "'isDefault' field is added on quality gate"),
        tuple("7.0", "'default' field on root level is deprecated"),
        tuple("7.0", "'isBuiltIn' field is added in the response"),
        tuple("7.0", "'actions' fields are added in the response"));
    assertThat(action.params()).extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("organization", false));
  }

}
