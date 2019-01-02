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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.stubbing.Answer;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.index.FileSourceTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.TestRequest;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class LinesActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ComponentDao componentDao = new ComponentDao();
  private SnapshotDao snapshotDao = new SnapshotDao();
  private ComponentDto privateProject;
  private OrganizationDto organization;
  private WsTester wsTester;

  @Before
  public void setUp() {
    HtmlSourceDecorator htmlSourceDecorator = mock(HtmlSourceDecorator.class);
    when(htmlSourceDecorator.getDecoratedSourceAsHtml(anyString(), anyString(), anyString())).then((Answer<String>)
      invocationOnMock -> "<p>" + invocationOnMock.getArguments()[0] + "</p>");
    SourceService sourceService = new SourceService(db.getDbClient(), htmlSourceDecorator);
    wsTester = new WsTester(new SourcesWs(
      new LinesAction(TestComponentFinder.from(db), db.getDbClient(), sourceService, htmlSourceDecorator, userSession)));
    organization = db.organizations().insert();
    privateProject = ComponentTesting.newPrivateProjectDto(organization);
  }

  @Test
  public void show_source() throws Exception {
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(3).build(), privateProject);
    setUserWithValidPermission(file);

    TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", file.uuid());
    request.execute().assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_to_show_source_if_no_source_found() throws Exception {
    ComponentDto file = insertFile(privateProject);
    setUserWithValidPermission(file);

    expectedException.expect(NotFoundException.class);
    wsTester.newGetRequest("api/sources", "lines").setParam("uuid", file.uuid()).execute();
  }

  @Test
  public void show_paginated_lines() throws Exception {
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(3).build(), privateProject);
    setUserWithValidPermission(file);

    wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .setParam("from", "3")
      .setParam("to", "3")
      .execute()
      .assertJson(getClass(), "show_paginated_lines.json");
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

    userSession.logIn("login")
      .addMembership(db.getDefaultOrganization())
      .addProjectPermission(UserRole.CODEVIEWER, project, file);

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("key", file.getKey())
      .setParam("branch", file.getBranch())
      .execute()
      .assertJson(getClass(), "show_source.json");
  }

  @Test
  public void pull_request() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST));
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(branch.uuid())
      .setFileUuid(file.uuid())
      .setSourceData(FileSourceTesting.newFakeData(3).build()));
    db.commit();

    userSession.logIn("login")
      .addMembership(db.getDefaultOrganization())
      .addProjectPermission(UserRole.CODEVIEWER, project, file);

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("key", file.getKey())
      .setParam("pullRequest", file.getPullRequest())
      .execute()
      .assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_when_no_uuid_or_key_param() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'uuid' or 'key' must be provided");

    TestRequest request = wsTester.newGetRequest("api/sources", "lines");
    request.execute();
  }

  @Test
  public void fail_when_file_key_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'Foo.java' not found");

    TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("key", "Foo.java");
    request.execute();
  }

  @Test
  public void fail_when_file_uuid_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'ABCD' not found");

    TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", "ABCD");
    request.execute();
  }

  @Test
  public void fail_when_file_is_removed() throws Exception {
    ComponentDto file = newFileDto(privateProject).setDbKey("file-key").setEnabled(false);
    db.components().insertComponents(privateProject, file);
    setUserWithValidPermission(file);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'file-key' not found");

    TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("key", "file-key");
    request.execute();
  }

  @Test(expected = ForbiddenException.class)
  public void check_permission() throws Exception {
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(1).build(), privateProject);

    userSession.logIn("login");

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .execute();
  }

  @Test
  public void display_deprecated_fields() throws Exception {
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(1).build(), privateProject);
    setUserWithValidPermission(file);

    wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "display_deprecated_fields.json");
  }

  @Test
  public void use_period_date_if_new_line_not_yet_available_in_db() throws Exception {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    dataBuilder.addLines(DbFileSources.Line.newBuilder().setLine(1).setScmDate(1000L).build());
    dataBuilder.addLines(DbFileSources.Line.newBuilder().setLine(2).setScmDate(2000L).build());
    // only this line should be considered as new
    dataBuilder.addLines(DbFileSources.Line.newBuilder().setLine(3).setScmDate(3000L).build());
    ComponentDto project = db.components().insertPrivateProject();
    insertPeriod(project, 2000L);
    ComponentDto file = insertFileWithData(dataBuilder.build(), project);
    setUserWithValidPermission(file);

    wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "generated_isNew.json");
  }

  @Test
  public void use_deprecated_overall_coverage_fields_if_exists() throws Exception {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    ComponentDto file = insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedOverallLineHits(1)
      .setDeprecatedOverallConditions(2)
      .setDeprecatedOverallCoveredConditions(3)
      .setDeprecatedUtLineHits(1)
      .setDeprecatedUtConditions(2)
      .setDeprecatedUtCoveredConditions(3)
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build(), privateProject);
    setUserWithValidPermission(file);

    wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void use_deprecated_ut_coverage_fields_if_exists() throws Exception {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    ComponentDto file = insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedUtLineHits(1)
      .setDeprecatedUtConditions(2)
      .setDeprecatedUtCoveredConditions(3)
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build(), privateProject);
    setUserWithValidPermission(file);

    TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid());

    request.execute().assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void use_deprecated_it_coverage_fields_if_exists() throws Exception {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    ComponentDto file = insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build(), privateProject);
    setUserWithValidPermission(file);

    TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid());

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
    expectedException.expectMessage("Parameter 'uuid' cannot be used at the same time as 'branch' or 'pullRequest'");

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

  @Test
  public void hide_scmAuthors_if_not_member_of_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(org);
    userSession.registerComponents(publicProject);

    DbFileSources.Data data = DbFileSources.Data.newBuilder()
      .addLines(newLineBuilder().setScmAuthor("isaac@asimov.com"))
      .build();

    ComponentDto file = insertFileWithData(data, publicProject);

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "hide_scmAuthors.json");
  }

  @Test
  public void show_scmAuthors_if_member_of_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    ComponentDto publicProject = db.components().insertPublicProject(org);
    UserDto user = db.users().insertUser();
    userSession.logIn(user)
      .registerComponents(publicProject)
      .addMembership(org);

    DbFileSources.Data data = DbFileSources.Data.newBuilder()
      .addLines(newLineBuilder().setScmAuthor("isaac@asimov.com"))
      .build();

    ComponentDto file = insertFileWithData(data, publicProject);

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "show_scmAuthors.json");
  }

  private ComponentDto insertFileWithData(DbFileSources.Data fileData, ComponentDto project) {
    ComponentDto file = insertFile(project);
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(project.projectUuid())
      .setFileUuid(file.uuid())
      .setSourceData(fileData));
    db.commit();
    return file;
  }

  private void setUserWithValidPermission(ComponentDto file) {
    userSession.logIn("login")
      .addProjectPermission(UserRole.CODEVIEWER, privateProject, file)
      .addMembership(organization);
  }

  private ComponentDto insertFile(ComponentDto project) {
    ComponentDto file = newFileDto(project);
    componentDao.insert(db.getSession(), file);
    db.getSession().commit();
    return file;
  }

  private DbFileSources.Line.Builder newLineBuilder() {
    return DbFileSources.Line.newBuilder()
      .setLine(1)
      .setScmRevision("REVISION_" + 1)
      .setScmAuthor("AUTHOR_" + 1)
      .setScmDate(1_500_000_000_00L)
      .setSource("SOURCE_" + 1);
  }

  private void insertPeriod(ComponentDto componentDto, long date) {
    SnapshotDto dto = new SnapshotDto();
    dto.setUuid("uuid");
    dto.setLast(true);
    dto.setPeriodDate(date);
    dto.setComponentUuid(componentDto.uuid());
    snapshotDao.insert(db.getSession(), dto);
  }
}
