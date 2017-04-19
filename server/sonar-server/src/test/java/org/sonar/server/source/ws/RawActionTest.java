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
package org.sonar.server.source.ws;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RawActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  @Mock
  SourceService sourceService;

  WsTester tester;

  ComponentDto project = ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto());
  ComponentDto file = ComponentTesting.newFileDto(project, null);

  @Before
  public void setUp() {
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.openSession(false)).thenReturn(session);
    tester = new WsTester(new SourcesWs(new RawAction(dbClient, sourceService, userSessionRule, new ComponentFinder(dbClient))));
  }

  @Test
  public void get_txt() throws Exception {
    String fileKey = "src/Foo.java";
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);
    when(componentDao.selectByKey(session, fileKey)).thenReturn(com.google.common.base.Optional.of(file));

    Iterable<String> lines = newArrayList(
      "public class HelloWorld {",
      "}");
    when(sourceService.getLinesAsRawText(session, file.uuid(), 1, Integer.MAX_VALUE)).thenReturn(Optional.of(lines));

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "raw").setParam("key", fileKey);
    String result = request.execute().outputAsString();
    assertThat(result).isEqualTo("public class HelloWorld {\n}\n");
  }

  @Test(expected = ForbiddenException.class)
  public void requires_code_viewer_permission() throws Exception {
    when(componentDao.selectByKey(session, "src/Foo.java")).thenReturn(com.google.common.base.Optional.of(file));
    tester.newGetRequest("api/sources", "raw").setParam("key", "src/Foo.java").execute();
  }
}
