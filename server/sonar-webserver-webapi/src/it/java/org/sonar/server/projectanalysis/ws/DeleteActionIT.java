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
package org.sonar.server.projectanalysis.ws;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;

public class DeleteActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(new DeleteAction(dbClient, userSession));

  @Test
  public void project_administrator_deletes_analysis() {
    ProjectData project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project.getMainBranchDto()).setUuid("A1").setLast(false).setStatus(STATUS_PROCESSED));
    db.components().insertSnapshot(newAnalysis(project.getMainBranchDto()).setUuid("A2").setLast(true).setStatus(STATUS_PROCESSED));
    logInAsProjectAdministrator(project);

    call("A1");

    db.commit();
    assertThat(dbClient.snapshotDao().selectByUuids(dbSession, newArrayList("A1", "A2"))).extracting(SnapshotDto::getUuid, SnapshotDto::getStatus).containsExactly(
      tuple("A1", STATUS_UNPROCESSED),
      tuple("A2", STATUS_PROCESSED));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("delete");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.param("analysis").isRequired()).isTrue();
  }

  @Test
  public void last_analysis_cannot_be_deleted() {
    ProjectData project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project.getMainBranchDto()).setUuid("A1").setLast(true));
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> call("A1"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The last analysis 'A1' cannot be deleted");
  }

  @Test
  public void fail_when_analysis_is_new_code_period_baseline() {
    String analysisUuid = RandomStringUtils.secure().nextAlphabetic(12);
    ProjectData project = db.components().insertPrivateProject();
    SnapshotDto analysis = db.components().insertSnapshot(newAnalysis(project.getMainBranchDto()).setUuid(analysisUuid).setLast(false));
    db.newCodePeriods().insert(new NewCodePeriodDto()
      .setProjectUuid(project.projectUuid())
      .setBranchUuid(project.mainBranchUuid())
      .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
      .setValue(analysis.getUuid()));
    db.commit();
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> call(analysisUuid))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The analysis '" + analysisUuid + "' can not be deleted because it is set as a new code period baseline");
  }

  @Test
  public void fail_when_analysis_not_found() {
    userSession.logIn().setSystemAdministrator();

    assertThatThrownBy(() -> call("A42"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Analysis 'A42' not found");
  }

  @Test
  public void fail_when_analysis_is_unprocessed() {
    ProjectData project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project.getMainBranchDto()).setUuid("A1").setLast(false).setStatus(STATUS_UNPROCESSED));
    logInAsProjectAdministrator(project);

    assertThatThrownBy(() -> call("A1"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Analysis 'A1' not found");
  }

  @Test
  public void fail_when_not_enough_permission() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(newAnalysis(project).setUuid("A1").setLast(false));
    userSession.logIn();

    assertThatThrownBy(() -> call("A1"))
      .isInstanceOf(ForbiddenException.class);
  }

  private void call(String analysis) {
    ws.newRequest()
      .setParam(PARAM_ANALYSIS, analysis)
      .execute();
  }

  private void logInAsProjectAdministrator(ProjectData project) {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());
  }
}
