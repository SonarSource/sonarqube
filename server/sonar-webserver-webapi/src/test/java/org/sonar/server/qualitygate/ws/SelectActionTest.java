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

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;

public class SelectActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private SelectAction underTest = new SelectAction(dbClient, TestComponentFinder.from(db),
    new QualityGatesWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db)));
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void select_by_id() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", project.getId().toString())
      .setParam("organization", organization.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void select_by_uuid() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", project.uuid())
      .setParam("organization", organization.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void select_by_key() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void change_quality_gate_for_project() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto initialQualityGate = db.qualityGates().insertQualityGate(organization);
    QGateWithOrgDto secondQualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    ws.newRequest()
      .setParam("gateId", initialQualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();

    ws.newRequest()
      .setParam("gateId", secondQualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();

    assertSelected(secondQualityGate, project);
  }

  @Test
  public void select_same_quality_gate_for_project_twice() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto initialQualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    ws.newRequest()
      .setParam("gateId", initialQualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();

    ws.newRequest()
      .setParam("gateId", initialQualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();

    assertSelected(initialQualityGate, project);
  }

  @Test
  public void project_admin() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    userSession.logIn().addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void gate_administrator_can_associate_a_gate_to_a_project() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    OrganizationDto organization = db.getDefaultOrganization();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void fail_when_project_belongs_to_another_organization() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(anotherOrganization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Project '%s' doesn't exist in organization '%s'", project.getKey(), organization.getKey()));

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_no_quality_gate() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    ComponentDto project = db.components().insertPrivateProject(organization);

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("gateId", "1")
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_no_project_id() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", String.valueOf((Long) 1L))
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_no_project_key() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(ADMINISTER_QUALITY_GATES, organization);
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);

    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", "unknown")
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_anonymous() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    userSession.anonymous();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_not_project_admin() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    userSession.logIn().addProjectPermission(ISSUE_ADMIN, project);

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_not_quality_gates_admin() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertPrivateProject(organization);
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", branch.getDbKey())
      .setParam("organization", organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_id() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", branch.uuid())
      .setParam("organization", organization.getKey())
      .execute();
  }

  private void assertSelected(QualityGateDto qualityGate, ComponentDto project) {
    Optional<String> qGateUuid = db.qualityGates().selectQGateUuidByComponentUuid(project.uuid());
    assertThat(qGateUuid)
      .isNotNull()
      .isNotEmpty()
      .hasValue(qualityGate.getUuid());
  }

}
