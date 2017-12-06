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
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;

public class DeselectActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private TestDefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);
  private DeselectAction underTest = new DeselectAction(dbClient, TestComponentFinder.from(db),
    new QualityGatesWsSupport(db.getDbClient(), userSession, organizationProvider));
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void deselect_by_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    associateProjectToQualityGate(project, qualityGate);

    ws.newRequest()
      .setParam("projectKey", project.getDbKey())
      .execute();

    assertDeselected(project.getId());
  }

  @Test
  public void deselect_by_uuid() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    associateProjectToQualityGate(project, qualityGate);

    ws.newRequest()
      .setParam("projectId", project.uuid())
      .execute();

    assertDeselected(project.getId());
  }

  @Test
  public void deselect_by_id() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    associateProjectToQualityGate(project, qualityGate);

    ws.newRequest()
      .setParam("projectId", valueOf(project.getId()))
      .execute();

    assertDeselected(project.getId());
  }

  @Test
  public void project_admin() {
    OrganizationDto organization = db.organizations().insert();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject(organization);
    associateProjectToQualityGate(project, qualityGate);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    ws.newRequest()
      .setParam("projectKey", project.getDbKey())
      .setParam("organization", organization.getKey())
      .execute();

    assertDeselected(project.getId());
  }

  @Test
  public void other_project_should_not_be_updated() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    String gateId = valueOf(qualityGate.getId());
    associateProjectToQualityGate(project, qualityGate);
    // Another project
    ComponentDto anotherProject = db.components().insertPrivateProject();
    associateProjectToQualityGate(anotherProject, qualityGate);

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .execute();

    assertDeselected(project.getId());
    assertSelected(gateId, anotherProject.getId());
  }

  @Test
  public void default_organization_is_used_when_no_organization_parameter() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject();
    associateProjectToQualityGate(project, qualityGate);

    ws.newRequest()
      .setParam("projectKey", project.getDbKey())
      .execute();

    assertDeselected(project.getId());
  }

  @Test
  public void fail_when_no_project_id() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("projectId", valueOf((Long) 1L))
      .execute();
  }

  @Test
  public void fail_when_no_project_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("projectKey", "unknown")
      .execute();
  }

  @Test
  public void fail_when_anonymous() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.anonymous();

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setParam("projectKey", project.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_not_project_admin() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.ISSUE_ADMIN, project);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("projectKey", project.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_not_quality_gates_admin() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES, db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject();

    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, project.getOrganizationUuid());

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("projectKey", project.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam("projectKey", branch.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_id() {
    ComponentDto project = db.components().insertMainBranch(db.getDefaultOrganization());
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    ws.newRequest()
      .setParam("projectId", branch.uuid())
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.description()).isNotEmpty();
    assertThat(def.isPost()).isTrue();
    assertThat(def.since()).isEqualTo("4.3");
    assertThat(def.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactly(
      tuple("6.6", "The parameter 'gateId' was removed"));

    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("projectKey", false),
        tuple("projectId", false));
  }

  private void associateProjectToQualityGate(ComponentDto project, QualityGateDto qualityGate) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setResourceId(project.getId())
      .setValue(qualityGate.getId().toString())
      .setKey(SONAR_QUALITYGATE_PROPERTY));
    db.commit();
  }

  private void assertDeselected(long projectId) {
    assertThat(dbClient.propertiesDao().selectProjectProperty(projectId, SONAR_QUALITYGATE_PROPERTY)).isNull();
  }

  private void assertSelected(String qGateId, long projectId) {
    assertThat(dbClient.propertiesDao().selectProjectProperty(projectId, SONAR_QUALITYGATE_PROPERTY).getValue()).isEqualTo(qGateId);
  }

}
