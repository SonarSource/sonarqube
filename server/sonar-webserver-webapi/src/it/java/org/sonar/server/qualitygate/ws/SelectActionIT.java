/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;

class SelectActionIT {

  @RegisterExtension
  public UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final ComponentFinder componentFinder = TestComponentFinder.from(db);
  private final SelectAction underTest = new SelectAction(dbClient,
    new QualityGatesWsSupport(db.getDbClient(), userSession, componentFinder));
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  void select_by_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  void change_quality_gate_for_project() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto initialQualityGate = db.qualityGates().insertQualityGate();
    QualityGateDto secondQualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, initialQualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute();

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, secondQualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute();

    assertSelected(secondQualityGate, project);
  }

  @Test
  void select_same_quality_gate_for_project_twice() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto initialQualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, initialQualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute();

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, initialQualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute();

    assertSelected(initialQualityGate, project);
  }

  @Test
  void project_admin() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  void gate_administrator_can_associate_a_gate_to_a_project() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute();

    assertSelected(qualityGate, project);
  }

  @Test
  void fail_when_no_quality_gate() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, "unknown")
      .setParam("projectKey", project.getKey())
      .execute())
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  void fail_when_no_project_key() {
    userSession.addPermission(ADMINISTER_QUALITY_GATES);
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam("projectKey", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  void fail_when_anonymous() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.anonymous();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void fail_when_not_project_admin() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectData project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(ISSUE_ADMIN, project.getProjectDto());

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam("projectKey", project.projectKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void fail_when_not_quality_gates_admin() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam(PARAM_GATE_NAME, qualityGate.getName())
      .setParam("projectKey", project.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  private void assertSelected(QualityGateDto qualityGate, ProjectDto project) {
    Optional<String> qGateUuid = db.qualityGates().selectQGateUuidByProjectUuid(project.getUuid());
    assertThat(qGateUuid)
      .isNotNull()
      .isNotEmpty()
      .hasValue(qualityGate.getUuid());
  }

}
