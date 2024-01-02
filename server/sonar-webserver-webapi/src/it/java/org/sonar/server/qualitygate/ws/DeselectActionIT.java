/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;

public class DeselectActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final ComponentFinder componentFinder = TestComponentFinder.from(db);
  private final DeselectAction underTest = new DeselectAction(dbClient, new QualityGatesWsSupport(db.getDbClient(), userSession, componentFinder));
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void deselect_by_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    associateProjectToQualityGate(project, qualityGate);

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .execute();

    assertDeselected(project);
  }

  @Test
  public void project_admin() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    associateProjectToQualityGate(project, qualityGate);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .execute();

    assertDeselected(project);
  }

  @Test
  public void other_project_should_not_be_updated() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    associateProjectToQualityGate(project, qualityGate);
    // Another project
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    associateProjectToQualityGate(anotherProject, qualityGate);

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .execute();

    assertDeselected(project);
    assertSelected(qualityGate, anotherProject);
  }

  @Test
  public void default_is_used() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    associateProjectToQualityGate(project, qualityGate);

    ws.newRequest()
      .setParam("projectKey", project.getKey())
      .execute();

    assertDeselected(project);
  }

  @Test
  public void fail_when_no_project_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("projectKey", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_anonymous() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.anonymous();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("projectKey", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_not_project_admin() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.ISSUE_ADMIN, project.getProjectDto());

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("projectKey", project.projectKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_not_quality_gates_admin() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES);
    assertThatThrownBy(() -> ws.newRequest()
      .setParam("projectKey", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.description()).isNotEmpty();
    assertThat(def.isPost()).isTrue();
    assertThat(def.since()).isEqualTo("4.3");
    assertThat(def.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactly(
      tuple("6.6", "The parameter 'gateId' was removed"),
      tuple("8.3", "The parameter 'projectId' was removed"));

    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("projectKey", true));
  }

  private void associateProjectToQualityGate(ProjectDto project, QualityGateDto qualityGate) {
    db.qualityGates().associateProjectToQualityGate(project, qualityGate);
    db.commit();
  }

  private void assertDeselected(ProjectDto project) {
    Optional<String> qGateUuid = db.qualityGates().selectQGateUuidByProjectUuid(project.getUuid());
    assertThat(qGateUuid)
      .isNotNull()
      .isEmpty();
  }

  private void assertSelected(QualityGateDto qualityGate, ProjectDto project) {
    Optional<String> qGateUuid = db.qualityGates().selectQGateUuidByProjectUuid(project.getUuid());
    assertThat(qGateUuid)
      .isNotNull()
      .isNotEmpty()
      .hasValue(qualityGate.getUuid());
  }
}
