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
package org.sonar.server.pushapi.sonarlint;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.pushapi.TestPushRequest;
import org.sonar.server.pushapi.WsPushActionTester;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.TestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarLintPushActionTest {

  private final SonarLintClientsRegistry registry = mock(SonarLintClientsRegistry.class);
  private final UserSession userSession = mock(UserSession.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final ProjectDao projectDao = mock(ProjectDao.class);
  private final SonarLintClientPermissionsValidator permissionsValidator = mock(SonarLintClientPermissionsValidator.class);

  private final WsPushActionTester ws = new WsPushActionTester(new SonarLintPushAction(registry, userSession, dbClient, permissionsValidator));

  @Before
  public void before() {
    List<ProjectDto> projectDtos = generateProjectDtos(2);
    when(projectDao.selectProjectsByKeys(any(), any())).thenReturn(projectDtos);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  public List<ProjectDto> generateProjectDtos(int howMany) {
    return IntStream.rangeClosed(1, howMany).mapToObj(i -> {
      ProjectDto dto = new ProjectDto();
      dto.setKee("project" + i);
      return dto;
    }).collect(Collectors.toList());
  }

  @Test
  public void defineTest() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("9.4");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("languages", true), tuple("projectKeys", true));
  }

  @Test
  public void handle_returnsNoResponseWhenParamsAndHeadersProvided() {
    TestResponse response = ws.newPushRequest()
      .setParam("projectKeys", "project1,project2")
      .setParam("languages", "java")
      .setHeader("accept", "text/event-stream")
      .execute();

    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void handle_whenAcceptHeaderNotProvided_statusCode406() {
    TestResponse testResponse = ws.newPushRequest().setParam("projectKeys", "project1,project2")
      .setParam("languages", "java")
      .execute();

    assertThat(testResponse.getStatus()).isEqualTo(406);
  }

  @Test
  public void handle_whenParamsNotProvided_throwException() {
    TestPushRequest testRequest = ws.newPushRequest()
      .setHeader("accept", "text/event-stream");

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'projectKeys' parameter is missing");
  }

  @Test
  public void handle_whenParamProjectKeyNotValid_throwException() {
    TestPushRequest testRequest = ws.newPushRequest()
      .setParam("projectKeys", "not-valid-key")
      .setParam("languages", "java")
      .setHeader("accept", "text/event-stream");
    when(projectDao.selectProjectsByKeys(any(), any())).thenReturn(List.of());

    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Param projectKeys is invalid.");
  }
}
