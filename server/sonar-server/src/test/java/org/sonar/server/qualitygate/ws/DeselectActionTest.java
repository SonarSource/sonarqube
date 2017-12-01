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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;

public class DeselectActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private TestDefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.from(db);
  private QualityGates qualityGates = new QualityGates(dbClient, userSession, organizationProvider, UuidFactoryFast.getInstance());
  private WsActionTester ws;
  private ComponentDto project;
  private QualityGateDto gate;
  private DeselectAction underTest;

  @Before
  public void setUp() {
    ComponentFinder componentFinder = TestComponentFinder.from(db);
    underTest = new DeselectAction(qualityGates, dbClient, componentFinder);
    ws = new WsActionTester(underTest);

    project = db.components().insertPrivateProject();
    gate = insertQualityGate();
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.description()).isNotEmpty();
    assertThat(def.isPost()).isTrue();
    assertThat(def.since()).isEqualTo("4.3");
    assertThat(def.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactly(
      tuple("6.6", "The parameter 'gateId' was removed"));

    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("projectId", "projectKey");

    WebService.Param projectId = def.param("projectId");
    assertThat(projectId.isRequired()).isFalse();
    assertThat(projectId.deprecatedSince()).isEqualTo("6.1");
    assertThat(projectId.description()).isNotEmpty();
    assertThat(projectId.exampleValue()).isNotEmpty();

    WebService.Param projectKey = def.param("projectKey");
    assertThat(projectKey.isRequired()).isFalse();
    assertThat(projectKey.since()).isEqualTo("6.1");
    assertThat(projectKey.description()).isNotEmpty();
    assertThat(projectKey.exampleValue()).isNotEmpty();
  }

  @Test
  public void deselect_by_id() throws Exception {
    logInAsRoot();

    ComponentDto anotherProject = db.components().insertPrivateProject();
    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);
    associateProjectToQualityGate(anotherProject.getId(), gateId);

    callById(project.getId());

    assertDeselected(project.getId());
    assertSelected(gateId, anotherProject.getId());
  }

  @Test
  public void deselect_by_uuid() throws Exception {
    logInAsRoot();

    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);

    callByUuid(project.uuid());

    assertDeselected(project.getId());
  }

  @Test
  public void deselect_by_key() throws Exception {
    logInAsRoot();

    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);

    callByKey(project.getDbKey());

    assertDeselected(project.getId());
  }

  @Test
  public void project_admin() throws Exception {
    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    callByKey(project.getDbKey());

    assertDeselected(project.getId());
  }

  @Test
  public void fail_when_no_project_id() throws Exception {
    String gateId = String.valueOf(gate.getId());

    expectedException.expect(NotFoundException.class);

    callById(1L);
  }

  @Test
  public void fail_when_no_project_key() throws Exception {
    String gateId = String.valueOf(gate.getId());

    expectedException.expect(NotFoundException.class);

    callByKey("unknown");
  }

  @Test
  public void fail_when_anonymous() throws Exception {
    String gateId = String.valueOf(gate.getId());
    userSession.anonymous();

    expectedException.expect(ForbiddenException.class);
    callByKey(project.getDbKey());
  }

  @Test
  public void fail_when_not_project_admin() throws Exception {
    String gateId = String.valueOf(gate.getId());

    userSession.logIn().addProjectPermission(UserRole.ISSUE_ADMIN, project);

    expectedException.expect(ForbiddenException.class);

    callByKey(project.getDbKey());
  }

  @Test
  public void fail_when_not_quality_gates_admin() throws Exception {
    String gateId = String.valueOf(gate.getId());

    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, project.getOrganizationUuid());

    expectedException.expect(ForbiddenException.class);

    callByKey(project.getDbKey());
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    String gateId = String.valueOf(gate.getId());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    callByKey(branch.getDbKey());
  }

  @Test
  public void fail_when_using_branch_id() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    String gateId = String.valueOf(gate.getId());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    callByUuid(branch.uuid());
  }

  private QualityGateDto insertQualityGate() {
    QualityGateDto gate = new QualityGateDto().setName("Custom").setUuid(Uuids.createFast());
    dbClient.qualityGateDao().insert(dbSession, gate);
    dbSession.commit();
    return gate;
  }

  private void callByKey(String projectKey) {
    ws.newRequest()
      .setParam("projectKey", projectKey)
      .execute();
  }

  private void callById(Long projectId) {
    ws.newRequest()
      .setParam("projectId", String.valueOf(projectId))
      .execute();
  }

  private void callByUuid(String projectUuid) {
    ws.newRequest()
      .setParam("projectId", projectUuid)
      .execute();
  }

  private void associateProjectToQualityGate(long projectId, String gateId) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setResourceId(projectId)
      .setValue(gateId)
      .setKey(SONAR_QUALITYGATE_PROPERTY));
    db.commit();
  }

  private void assertDeselected(long projectId) {
    assertThat(dbClient.propertiesDao().selectProjectProperty(projectId, SONAR_QUALITYGATE_PROPERTY)).isNull();
  }

  private void assertSelected(String qGateId, long projectId) {
    assertThat(dbClient.propertiesDao().selectProjectProperty(projectId, SONAR_QUALITYGATE_PROPERTY).getValue()).isEqualTo(qGateId);
  }

  private void logInAsRoot() {
    userSession.logIn().setRoot();
  }
}
