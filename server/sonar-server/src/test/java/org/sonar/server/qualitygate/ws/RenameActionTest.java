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
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.QualityGate;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class RenameActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private WsActionTester ws = new WsActionTester(
    new RenameAction(db.getDbClient(), new QualityGatesWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider)));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("rename");
    assertThat(action.since()).isEqualTo("4.3");
    assertThat(action.changelog()).isEmpty();
    assertThat(action.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", true),
        tuple("name", true),
        tuple("organization", false));
  }

  @Test
  public void rename() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("old name"));
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);

    ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("name", "new name")
      .setParam("organization", organization.getKey())
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectById(db.getSession(), qualityGate.getId()).getName()).isEqualTo("new name");
  }

  @Test
  public void response_contains_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("old name"));

    QualityGate result = ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("name", "new name")
      .setParam("organization", organization.getKey())
      .executeProtobuf(QualityGate.class);

    assertThat(result.getId()).isEqualTo(qualityGate.getId());
    assertThat(result.getName()).isEqualTo("new name");
  }

  @Test
  public void rename_with_same_name() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("name"));

    ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("name", "name")
      .setParam("organization", organization.getKey())
      .execute();

    assertThat(db.getDbClient().qualityGateDao().selectById(db.getSession(), qualityGate.getId()).getName()).isEqualTo("name");
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto otherQualityGate = db.qualityGates().insertQualityGate(otherOrganization);
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());

    QualityGate result = ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("name", "new name")
      .executeProtobuf(QualityGate.class);

    assertThat(result.getId()).isEqualTo(qualityGate.getId());
    assertThat(result.getName()).isEqualTo("new name");
  }

  @Test
  public void fail_on_built_in_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setBuiltIn(true));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName()));

    ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("name", "name")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_on_empty_name() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("name", "")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_using_existing_name() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate1 = db.qualityGates().insertQualityGate(organization);
    QGateWithOrgDto qualityGate2 = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("Name '%s' has already been taken", qualityGate2.getName()));

    ws.newRequest()
      .setParam("id", qualityGate1.getId().toString())
      .setParam("name", qualityGate2.getName())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_on_unknown_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("id", "123")
      .setParam("name", "new name")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_not_quality_gates_administer() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn("john").addPermission(ADMINISTER_QUALITY_PROFILES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization, qg -> qg.setName("old name"));

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("id", qualityGate.getId().toString())
      .setParam("name", "new name")
      .setParam("organization", organization.getKey())
      .execute();
  }

}
