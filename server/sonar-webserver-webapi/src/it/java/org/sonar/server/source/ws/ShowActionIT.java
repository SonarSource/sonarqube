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
package org.sonar.server.source.ws;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShowActionIT {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private SourceService sourceService = mock(SourceService.class);
  private DbClient dbClient = mock(DbClient.class);
  private DbSession session = mock(DbSession.class);
  private ComponentDao componentDao = mock(ComponentDao.class);
  private BranchDao branchDao = mock(BranchDao.class);
  private ProjectDto project = ComponentTesting.newProjectDto();
  private ComponentDto mainBranchComponentDto = ComponentTesting.newBranchComponent(project, ComponentTesting.newMainBranchDto(project.getUuid()));
  private ComponentDto file = ComponentTesting.newFileDto(mainBranchComponentDto);
  private ShowAction underTest = new ShowAction(sourceService, dbClient, userSessionRule,
    new ComponentFinder(dbClient, new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT)));
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.openSession(false)).thenReturn(session);
  }

  @Test
  public void show_source() {
    String fileKey = "src/Foo.java";
    userSessionRule.addProjectPermission(ProjectPermission.CODEVIEWER, project)
      .addProjectBranchMapping(project.getUuid(), mainBranchComponentDto);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));
    when(sourceService.getLinesAsHtml(eq(session), eq(file.uuid()), anyInt(), anyInt())).thenReturn(Optional.of(newArrayList(
      "/*",
      " * Header",
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {",
      "}")));

    tester.newRequest()
      .setParam("key", fileKey)
      .execute()
      .assertJson(getClass(), "show_source.json");
  }

  @Test
  public void show_source_with_from_and_to_params() {
    String fileKey = "src/Foo.java";
    userSessionRule.addProjectPermission(ProjectPermission.CODEVIEWER, project)
      .addProjectBranchMapping(project.getUuid(), mainBranchComponentDto);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));
    when(sourceService.getLinesAsHtml(session, file.uuid(), 3, 5)).thenReturn(Optional.of(newArrayList(
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {")));
    tester.newRequest()
      .setParam("key", fileKey)
      .setParam("from", "3")
      .setParam("to", "5")
      .execute()
      .assertJson(getClass(), "show_source_with_params_from_and_to.json");
  }

  @Test
  public void show_source_accept_from_less_than_one() {
    String fileKey = "src/Foo.java";
    userSessionRule.addProjectPermission(ProjectPermission.CODEVIEWER, project)
      .addProjectBranchMapping(project.getUuid(), mainBranchComponentDto);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));
    when(sourceService.getLinesAsHtml(session, file.uuid(), 1, 5)).thenReturn(Optional.of(newArrayList(
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {")));
    tester.newRequest()
      .setParam("key", fileKey)
      .setParam("from", "0")
      .setParam("to", "5")
      .execute();
    verify(sourceService).getLinesAsHtml(session, file.uuid(), 1, 5);
  }

  @Test
  public void require_code_viewer() {
    String fileKey = "src/Foo.java";
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));

    assertThatThrownBy(() -> tester.newRequest().setParam("key", fileKey).execute())
      .isInstanceOf(ForbiddenException.class);
  }
}
