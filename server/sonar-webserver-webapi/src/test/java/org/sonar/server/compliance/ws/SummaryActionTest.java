/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.compliance.ws;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SummaryActionTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final ComponentFinder componentFinder = mock(ComponentFinder.class);
  private final UserSession userSession = mock(UserSession.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final ProjectDao projectDao = mock(ProjectDao.class);
  private final BranchDao branchDao = mock(BranchDao.class);

  private final SummaryAction underTest = new SummaryAction(dbClient, componentFinder, userSession);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
  }

  @Test
  public void define_configuresActionCorrectly() {
    WebService.Action def = tester.getDef();

    assertThat(def.key()).isEqualTo("summary");
    assertThat(def.since()).isEqualTo("10.7");
    assertThat(def.description()).isNotEmpty();
    assertThat(def.param("project")).isNotNull();
    assertThat(def.param("branch")).isNotNull();
  }

  @Test
  public void handle_whenProjectNotFound_throwsNotFoundException() {
    when(projectDao.selectProjectByKey(dbSession, "unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("project", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'unknown' not found");
  }

  @Test
  public void handle_whenProjectExists_returnsComplianceDigestSummary() {
    ProjectDto project = mock(ProjectDto.class);
    when(project.getKey()).thenReturn("my_project");
    when(project.getName()).thenReturn("My Project");
    when(project.getUuid()).thenReturn("proj-uuid-1");
    when(projectDao.selectProjectByKey(dbSession, "my_project")).thenReturn(Optional.of(project));

    TestResponse response = tester.newRequest()
      .setParam("project", "my_project")
      .setParam("branch", "main")
      .execute();

    verify(userSession).checkEntityPermission(ProjectPermission.USER, project);
    assertThat(response.getInput()).contains("\"projectKey\":\"my_project\"");
    assertThat(response.getInput()).contains("\"projectName\":\"My Project\"");
    assertThat(response.getInput()).contains("\"overallScore\":93.5");
    assertThat(response.getInput()).contains("\"status\":\"COMPLIANT\"");
    assertThat(response.getInput()).contains("OWASP Top 10");
  }
}
