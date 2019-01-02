/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShowActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  SourceService sourceService = mock(SourceService.class);

  WsTester tester;

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  ComponentDto project = ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto());
  ComponentDto file = ComponentTesting.newFileDto(project, null);

  @Before
  public void setUp() {
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.openSession(false)).thenReturn(session);
    tester = new WsTester(new SourcesWs(new ShowAction(sourceService, dbClient, userSessionRule,
      new ComponentFinder(dbClient, new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT)))));
  }

  @Test
  public void show_source() throws Exception {
    String fileKey = "src/Foo.java";
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));
    when(sourceService.getLinesAsHtml(eq(session), eq(file.uuid()), anyInt(), anyInt())).thenReturn(Optional.of(newArrayList(
      "/*",
      " * Header",
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {",
      "}")));

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "show").setParam("key", fileKey);
    request.execute().assertJson(getClass(), "show_source.json");
  }

  @Test
  public void show_source_with_from_and_to_params() throws Exception {
    String fileKey = "src/Foo.java";
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));
    when(sourceService.getLinesAsHtml(session, file.uuid(), 3, 5)).thenReturn(Optional.of(newArrayList(
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {")));
    WsTester.TestRequest request = tester
      .newGetRequest("api/sources", "show")
      .setParam("key", fileKey)
      .setParam("from", "3")
      .setParam("to", "5");
    request.execute().assertJson(getClass(), "show_source_with_params_from_and_to.json");
  }

  @Test
  public void show_source_accept_from_less_than_one() throws Exception {
    String fileKey = "src/Foo.java";
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));
    when(sourceService.getLinesAsHtml(session, file.uuid(), 1, 5)).thenReturn(Optional.of(newArrayList(
      " */",
      "",
      "public class <span class=\"sym-31 sym\">HelloWorld</span> {")));
    WsTester.TestRequest request = tester
      .newGetRequest("api/sources", "show")
      .setParam("key", fileKey)
      .setParam("from", "0")
      .setParam("to", "5");
    request.execute();
    verify(sourceService).getLinesAsHtml(session, file.uuid(), 1, 5);
  }

  @Test(expected = ForbiddenException.class)
  public void require_code_viewer() throws Exception {
    String fileKey = "src/Foo.java";
    when(componentDao.selectByKey(session, fileKey)).thenReturn(Optional.of(file));
    tester.newGetRequest("api/sources", "show").setParam("key", fileKey).execute();
  }
}
