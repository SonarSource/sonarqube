/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.source.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.index.FileSourceTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LinesActionTest {

  private static final String PROJECT_UUID = "abcd";
  private static final String FILE_UUID = "efgh";
  private static final String FILE_KEY = "Foo.java";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  SourceService sourceService;

  HtmlSourceDecorator htmlSourceDecorator;

  ComponentDao componentDao;

  WsTester wsTester;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    htmlSourceDecorator = mock(HtmlSourceDecorator.class);
    when(htmlSourceDecorator.getDecoratedSourceAsHtml(anyString(), anyString(), anyString())).then(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocationOnMock) throws Throwable {
        return "<p>" + invocationOnMock.getArguments()[0] + "</p>";
      }
    });
    sourceService = new SourceService(dbTester.getDbClient(), htmlSourceDecorator);
    componentDao = new ComponentDao();
    wsTester = new WsTester(new SourcesWs(
      new LinesAction(new ComponentFinder(dbTester.getDbClient()), dbTester.getDbClient(), sourceService, htmlSourceDecorator, userSessionRule)));
  }

  @Test
  public void show_source() throws Exception {
    newFile();

    dbTester.getDbClient().fileSourceDao().insert(new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(FileSourceTesting.newFakeData(3).build()));

    userSessionRule.login("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_to_show_source_if_no_source_found() {
    newFile();

    userSessionRule.login("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    try {
      WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", FILE_UUID);
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void show_paginated_lines() throws Exception {
    newFile();

    userSessionRule.login("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    dbTester.getDbClient().fileSourceDao().insert(new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(FileSourceTesting.newFakeData(3).build()));

    WsTester.TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID)
      .setParam("from", "3")
      .setParam("to", "3");
    request.execute().assertJson(getClass(), "show_paginated_lines.json");
  }

  @Test
  public void fail_when_no_uuid_or_key_param() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Either 'uuid' or 'key' must be provided, not both");

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines");
    request.execute();
  }

  @Test
  public void fail_when_file_key_does_not_exist() throws Exception {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Component key 'Foo.java' not found");

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("key", FILE_KEY);
    request.execute();
  }

  @Test
  public void fail_when_file_uuid_does_not_exist() throws Exception {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Component id 'ABCD' not found");

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", "ABCD");
    request.execute();
  }

  @Test(expected = ForbiddenException.class)
  public void should_check_permission() throws Exception {
    newFile();

    userSessionRule.login("login");

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID)
      .execute();
  }

  private void newFile() {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID);
    ComponentDto file = ComponentTesting.newFileDto(project, FILE_UUID).setKey(FILE_KEY);
    componentDao.insert(dbTester.getSession(), project, file);
    dbTester.getSession().commit();
  }
}
