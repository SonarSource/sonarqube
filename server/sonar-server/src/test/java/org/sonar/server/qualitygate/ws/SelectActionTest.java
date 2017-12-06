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
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;

public class SelectActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private SelectAction underTest = new SelectAction(dbClient, userSession, TestComponentFinder.from(db));
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void select_by_id() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", project.getId().toString())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void select_by_uuid() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", project.uuid())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void select_by_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void project_admin() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void gate_administrator_can_associate_a_gate_to_a_project() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES, project.getOrganizationUuid());

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  public void fail_when_no_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject();

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("gateId", String.valueOf("1"))
      .setParam("projectKey", project.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_no_project_id() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", String.valueOf((Long) 1L))
      .execute();
  }

  @Test
  public void fail_when_no_project_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", "unknown")
      .execute();
  }

  @Test
  public void fail_when_anonymous() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();

    userSession.anonymous();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_not_project_admin() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.ISSUE_ADMIN, project);

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_not_quality_gates_admin() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", project.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectKey", branch.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_id() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    ws.newRequest()
      .setParam("gateId", qualityGate.getId().toString())
      .setParam("projectId", branch.uuid())
      .execute();
  }

  private void assertSelected(QualityGateDto qualityGate, ComponentDto project) {
    assertThat(dbClient.propertiesDao().selectProjectProperty(project.getId(), SONAR_QUALITYGATE_PROPERTY).getValue()).isEqualTo(qualityGate.getId().toString());
  }

}
