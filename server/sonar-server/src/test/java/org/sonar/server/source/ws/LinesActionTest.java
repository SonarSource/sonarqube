/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.io.IOException;
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
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.index.FileSourceTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static java.lang.String.format;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class LinesActionTest {

  private static final String PROJECT_UUID = "abcd";
  private static final String FILE_UUID = "efgh";
  private static final String FILE_KEY = "Foo.java";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  SourceService sourceService;
  HtmlSourceDecorator htmlSourceDecorator;
  ComponentDao componentDao;

  ComponentDto project;
  ComponentDto file;

  WsTester wsTester;

  @Before
  public void setUp() {
    htmlSourceDecorator = mock(HtmlSourceDecorator.class);
    when(htmlSourceDecorator.getDecoratedSourceAsHtml(anyString(), anyString(), anyString())).then(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocationOnMock) {
        return "<p>" + invocationOnMock.getArguments()[0] + "</p>";
      }
    });
    sourceService = new SourceService(db.getDbClient(), htmlSourceDecorator);
    componentDao = new ComponentDao();
    wsTester = new WsTester(new SourcesWs(
      new LinesAction(TestComponentFinder.from(db), db.getDbClient(), sourceService, htmlSourceDecorator, userSession)));
    project = ComponentTesting.newPrivateProjectDto(db.organizations().insert(), PROJECT_UUID);
    file = newFileDto(project, null, FILE_UUID).setDbKey(FILE_KEY);
  }

  @Test
  public void show_source() throws Exception {
    insertFileWithData(FileSourceTesting.newFakeData(3).build());
    setUserWithValidPermission();

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_to_show_source_if_no_source_found() throws Exception {
    setUserWithValidPermission();
    insertFile();

    expectedException.expect(NotFoundException.class);
    wsTester.newGetRequest("api/sources", "lines").setParam("uuid", FILE_UUID).execute();
  }

  @Test
  public void show_paginated_lines() throws Exception {
    setUserWithValidPermission();
    insertFileWithData(FileSourceTesting.newFakeData(3).build());

    WsTester.TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID)
      .setParam("from", "3")
      .setParam("to", "3");
    request.execute().assertJson(getClass(), "show_paginated_lines.json");
  }

  @Test
  public void branch() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(branch.uuid())
      .setFileUuid(file.uuid())
      .setSourceData(FileSourceTesting.newFakeData(3).build()));
    db.commit();
    userSession.logIn("login").addProjectPermission(UserRole.CODEVIEWER, project, file);

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines")
      .setParam("key", file.getKey())
      .setParam("branch", file.getBranch());

    request.execute().assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_when_no_uuid_or_key_param() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'uuid' or 'key' must be provided");

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines");
    request.execute();
  }

  @Test
  public void fail_when_file_key_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'Foo.java' not found");

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("key", FILE_KEY);
    request.execute();
  }

  @Test
  public void fail_when_file_uuid_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'ABCD' not found");

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", "ABCD");
    request.execute();
  }

  @Test
  public void fail_when_file_is_removed() throws Exception {
    ComponentDto file = newFileDto(project).setDbKey("file-key").setEnabled(false);
    db.components().insertComponents(project, file);
    setUserWithValidPermission();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'file-key' not found");

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("key", "file-key");
    request.execute();
  }

  @Test(expected = ForbiddenException.class)
  public void check_permission() throws Exception {
    insertFileWithData(FileSourceTesting.newFakeData(1).build());

    userSession.logIn("login");

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID)
      .execute();
  }

  @Test
  public void display_deprecated_fields() throws Exception {
    insertFileWithData(FileSourceTesting.newFakeData(1).build());
    setUserWithValidPermission();

    WsTester.TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID);

    request.execute().assertJson(getClass(), "display_deprecated_fields.json");
  }

  @Test
  public void use_deprecated_overall_coverage_fields_if_exists() throws Exception {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedOverallLineHits(1)
      .setDeprecatedOverallConditions(2)
      .setDeprecatedOverallCoveredConditions(3)
      .setDeprecatedUtLineHits(1)
      .setDeprecatedUtConditions(2)
      .setDeprecatedUtCoveredConditions(3)
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build());
    setUserWithValidPermission();

    WsTester.TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID);

    request.execute().assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void use_deprecated_ut_coverage_fields_if_exists() throws Exception {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedUtLineHits(1)
      .setDeprecatedUtConditions(2)
      .setDeprecatedUtCoveredConditions(3)
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build());
    setUserWithValidPermission();

    WsTester.TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID);

    request.execute().assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void use_deprecated_it_coverage_fields_if_exists() throws Exception {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build());
    setUserWithValidPermission();

    WsTester.TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID);

    request.execute().assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void fail_if_branch_does_not_exist() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", file.getKey(), "another_branch"));

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("key", file.getKey())
      .setParam("branch", "another_branch")
      .execute();
  }

  @Test
  public void fail_when_uuid_and_branch_params_are_used_together() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'uuid' and 'branch' parameters cannot be used at the same time");

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .setParam("branch", "another_branch")
      .execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    userSession.addProjectPermission(UserRole.USER, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("key", branch.getDbKey())
      .execute();
  }

  @Test
  public void fail_when_using_branch_uuid() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    userSession.addProjectPermission(UserRole.USER, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component id '%s' not found", branch.uuid()));

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", branch.uuid())
      .execute();
  }

  private void insertFileWithData(DbFileSources.Data fileData) throws IOException {
    insertFile();
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(fileData));
    db.commit();
  }

  private void setUserWithValidPermission() {
    userSession.logIn("login").addProjectPermission(UserRole.CODEVIEWER, project, file);
  }

  private void insertFile() {
    componentDao.insert(db.getSession(), project, file);
    db.getSession().commit();
  }

  private DbFileSources.Line.Builder newLineBuilder() {
    return DbFileSources.Line.newBuilder()
      .setLine(1)
      .setScmRevision("REVISION_" + 1)
      .setScmAuthor("AUTHOR_" + 1)
      .setScmDate(1_500_000_000_00L)
      .setSource("SOURCE_" + 1);
  }
}
