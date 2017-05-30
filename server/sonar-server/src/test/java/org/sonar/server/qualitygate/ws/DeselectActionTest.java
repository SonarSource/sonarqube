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
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
  private QualityGates qualityGates = new QualityGates(dbClient, mock(MetricFinder.class), userSession);
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
  public void deselect_by_id() throws Exception {
    logInAsRoot();

    ComponentDto anotherProject = db.components().insertPrivateProject();
    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);
    associateProjectToQualityGate(anotherProject.getId(), gateId);

    callById(gateId, project.getId());

    assertDeselected(project.getId());
    assertSelected(gateId, anotherProject.getId());
  }

  @Test
  public void deselect_by_uuid() throws Exception {
    logInAsRoot();

    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);

    callByUuid(gateId, project.uuid());

    assertDeselected(project.getId());
  }

  @Test
  public void deselect_by_key() throws Exception {
    logInAsRoot();

    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);

    callByKey(gateId, project.getKey());

    assertDeselected(project.getId());
  }

  @Test
  public void project_admin() throws Exception {
    String gateId = String.valueOf(gate.getId());
    associateProjectToQualityGate(project.getId(), gateId);

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    callByKey(gateId, project.getKey());

    assertDeselected(project.getId());
  }

  @Test
  public void fail_when_no_quality_gate() throws Exception {
    expectedException.expect(NotFoundException.class);

    callByKey("-1", project.getKey());
  }

  @Test
  public void fail_when_no_project_id() throws Exception {
    String gateId = String.valueOf(gate.getId());

    expectedException.expect(NotFoundException.class);

    callById(gateId, 1L);
  }

  @Test
  public void fail_when_no_project_key() throws Exception {
    String gateId = String.valueOf(gate.getId());

    expectedException.expect(NotFoundException.class);

    callByKey(gateId, "unknown");
  }

  @Test
  public void fail_when_anonymous() throws Exception {
    String gateId = String.valueOf(gate.getId());
    userSession.anonymous();

    expectedException.expect(ForbiddenException.class);
    callByKey(gateId, project.getKey());
  }

  @Test
  public void fail_when_not_project_admin() throws Exception {
    String gateId = String.valueOf(gate.getId());

    userSession.logIn().addProjectPermission(UserRole.ISSUE_ADMIN, project);

    expectedException.expect(ForbiddenException.class);

    callByKey(gateId, project.getKey());
  }

  @Test
  public void fail_when_not_quality_gates_admin() throws Exception {
    String gateId = String.valueOf(gate.getId());

    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, project.getOrganizationUuid());

    expectedException.expect(ForbiddenException.class);

    callByKey(gateId, project.getKey());
  }

  private QualityGateDto insertQualityGate() {
    QualityGateDto gate = new QualityGateDto().setName("Custom");
    dbClient.qualityGateDao().insert(dbSession, gate);
    dbSession.commit();
    return gate;
  }

  private void callByKey(String gateId, String projectKey) {
    ws.newRequest()
      .setParam("gateId", String.valueOf(gateId))
      .setParam("projectKey", projectKey)
      .execute();
  }

  private void callById(String gateId, Long projectId) {
    ws.newRequest()
      .setParam("gateId", String.valueOf(gateId))
      .setParam("projectId", String.valueOf(projectId))
      .execute();
  }

  private void callByUuid(String gateId, String projectUuid) {
    ws.newRequest()
      .setParam("gateId", String.valueOf(gateId))
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
