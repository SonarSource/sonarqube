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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
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
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class LinesActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final ComponentDao componentDao = new ComponentDao(new NoOpAuditPersister());
  private final SnapshotDao snapshotDao = new SnapshotDao();
  private final HtmlSourceDecorator htmlSourceDecorator = mock(HtmlSourceDecorator.class);
  private final SourceService sourceService = new SourceService(db.getDbClient(), htmlSourceDecorator);
  private final LinesJsonWriter linesJsonWriter = new LinesJsonWriter(htmlSourceDecorator);
  private final LinesAction underTest = new LinesAction(TestComponentFinder.from(db), db.getDbClient(), sourceService, linesJsonWriter,
    userSession);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(htmlSourceDecorator.getDecoratedSourceAsHtml(anyString(), anyString(), anyString()))
      .then((Answer<String>) invocationOnMock -> "<p>" + invocationOnMock.getArguments()[0] + "</p>");
  }

  @Test
  public void show_source() {
    ProjectData privateProject = db.components().insertPrivateProject();
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(3).build(), privateProject.getMainBranchComponent());
    setUserWithValidPermission(privateProject);

    TestResponse response = tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute();

    response.assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_to_show_source_if_no_source_found() {
    ProjectData privateProject = db.components().insertPrivateProject();
    ComponentDto file = insertFile(privateProject.getMainBranchComponent());
    setUserWithValidPermission(privateProject);

    TestRequest request = tester.newRequest()
      .setParam("uuid", file.uuid());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void show_paginated_lines() {
    ProjectData privateProject = db.components().insertPrivateProject();
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(3).build(), privateProject.getMainBranchComponent());
    setUserWithValidPermission(privateProject);

    tester
      .newRequest()
      .setParam("uuid", file.uuid())
      .setParam("from", "3")
      .setParam("to", "3")
      .execute()
      .assertJson(getClass(), "show_paginated_lines.json");
  }

  @Test
  public void branch() {
    ProjectData project = db.components().insertPrivateProject();

    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setKey(branchName));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.mainBranchUuid()));
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(branch.uuid())
      .setFileUuid(file.uuid())
      .setSourceData(FileSourceTesting.newFakeData(3).build()));
    db.commit();

    userSession.logIn("login")
      .addProjectPermission(UserRole.USER, project.getProjectDto())
      .addProjectBranchMapping(project.projectUuid(), branch)
      .addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto());

    tester.newRequest()
      .setParam("key", file.getKey())
      .setParam("branch", branchName)
      .execute()
      .assertJson(getClass(), "show_source.json");
  }

  @Test
  public void pull_request() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    String pullRequestKey = secure().nextAlphanumeric(100);
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(branch.uuid())
      .setFileUuid(file.uuid())
      .setSourceData(FileSourceTesting.newFakeData(3).build()));
    db.commit();

    userSession.logIn("login")
      .addProjectPermission(UserRole.USER, projectData.getProjectDto())
      .addProjectPermission(UserRole.CODEVIEWER, projectData.getProjectDto())
      .addProjectBranchMapping(projectData.projectUuid(), branch);

    tester.newRequest()
      .setParam("key", file.getKey())
      .setParam("pullRequest", pullRequestKey)
      .execute()
      .assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_when_no_uuid_or_key_param() {
    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Either 'uuid' or 'key' must be provided");
  }

  @Test
  public void fail_when_file_key_does_not_exist() {
    assertThatThrownBy(() -> tester.newRequest().setParam("key", "Foo.java").execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Component key 'Foo.java' not found");
  }

  @Test
  public void fail_when_file_uuid_does_not_exist() {
    assertThatThrownBy(() -> tester.newRequest().setParam("uuid", "ABCD").execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Component id 'ABCD' not found");
  }

  @Test
  public void fail_when_file_is_removed() {
    ProjectData privateProject = db.components().insertPrivateProject();
    ComponentDto file = newFileDto(privateProject.getMainBranchComponent()).setKey("file-key").setEnabled(false);
    db.components().insertComponents(file);
    setUserWithValidPermission(privateProject);

    assertThatThrownBy(() -> tester.newRequest().setParam("key", "file-key").execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Component key 'file-key' not found");
  }

  @Test
  public void check_permission() {
    ComponentDto privateProject = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(1).build(), privateProject);

    userSession.logIn("login");

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("uuid", file.uuid())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void display_deprecated_fields() {
    ProjectData privateProject = db.components().insertPrivateProject();
    ComponentDto file = insertFileWithData(FileSourceTesting.newFakeData(1).build(), privateProject.getMainBranchComponent());
    setUserWithValidPermission(privateProject);

    tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "display_deprecated_fields.json");
  }

  @Test
  public void use_period_date_if_new_line_not_yet_available_in_db() {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    dataBuilder.addLines(DbFileSources.Line.newBuilder().setLine(1).setScmDate(1000L).build());
    dataBuilder.addLines(DbFileSources.Line.newBuilder().setLine(2).setScmDate(2000L).build());
    // only this line should be considered as new
    dataBuilder.addLines(DbFileSources.Line.newBuilder().setLine(3).setScmDate(3000L).build());
    ProjectData project = db.components().insertPrivateProject();
    insertPeriod(project.getMainBranchComponent(), 2000L);
    ComponentDto file = insertFileWithData(dataBuilder.build(), project.getMainBranchComponent());
    setUserWithValidPermission(project);

    tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "generated_isNew.json");
  }

  @Test
  public void use_deprecated_overall_coverage_fields_if_exists() {
    ProjectData privateProject = db.components().insertPrivateProject();
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
      .setDeprecatedItCoveredConditions(3)).build(), privateProject.getMainBranchComponent());
    setUserWithValidPermission(privateProject);

    tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void use_deprecated_ut_coverage_fields_if_exists() {
    ProjectData privateProject = db.components().insertPrivateProject();
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    ComponentDto file = insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedUtLineHits(1)
      .setDeprecatedUtConditions(2)
      .setDeprecatedUtCoveredConditions(3)
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build(), privateProject.getMainBranchComponent());
    setUserWithValidPermission(privateProject);

    tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void use_deprecated_it_coverage_fields_if_exists() {
    ProjectData privateProject = db.components().insertPrivateProject();
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    ComponentDto file = insertFileWithData(dataBuilder.addLines(newLineBuilder()
      .setDeprecatedItLineHits(1)
      .setDeprecatedItConditions(2)
      .setDeprecatedItCoveredConditions(3)).build(), privateProject.getMainBranchComponent());
    setUserWithValidPermission(privateProject);

    tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "convert_deprecated_data.json");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    db.components().insertProjectBranch(project.getProjectDto(), b -> b.setKey("my_branch"));

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("key", file.getKey())
      .setParam("branch", "another_branch")
      .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(String.format("Component '%s' on branch '%s' not found", file.getKey(), "another_branch"));
  }

  @Test
  public void fail_when_uuid_and_branch_params_are_used_together() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    db.components().insertProjectBranch(project.getProjectDto(), b -> b.setKey("my_branch"));

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("uuid", file.uuid())
      .setParam("branch", "another_branch")
      .execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Parameter 'uuid' cannot be used at the same time as 'branch' or 'pullRequest'");
  }

  @Test
  public void fail_when_using_branch_uuid() {
    ProjectData project = db.components().insertPrivateProject();
    BranchDto branch = db.components().insertProjectBranch(project.getProjectDto());
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());

    assertThatThrownBy(() -> tester.newRequest()
      .setParam("uuid", branch.getUuid())
      .execute())
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(format("Component id '%s' not found", branch.getUuid()));
  }

  @Test
  public void hide_scmAuthors() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();

    userSession.registerProjects(projectData.getProjectDto());
    userSession.addProjectBranchMapping(projectData.projectUuid(), mainBranch);

    DbFileSources.Data data = DbFileSources.Data.newBuilder()
      .addLines(newLineBuilder().setScmAuthor("isaac@asimov.com"))
      .build();

    ComponentDto file = insertFileWithData(data, mainBranch);

    String response = tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .getInput();

    assertThat(response).doesNotContain("isaac@asimov.com");
  }

  @Test
  public void show_scmAuthors() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).registerProjects(projectData.getProjectDto());
    userSession.addProjectBranchMapping(projectData.projectUuid(), mainBranch);

    DbFileSources.Data data = DbFileSources.Data.newBuilder()
      .addLines(newLineBuilder().setScmAuthor("isaac@asimov.com"))
      .build();

    ComponentDto file = insertFileWithData(data, mainBranch);

    tester.newRequest()
      .setParam("uuid", file.uuid())
      .execute()
      .assertJson(getClass(), "show_scmAuthors.json");
  }

  private ComponentDto insertFileWithData(DbFileSources.Data fileData, ComponentDto project) {
    ComponentDto file = insertFile(project);
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(project.branchUuid())
      .setFileUuid(file.uuid())
      .setSourceData(fileData));
    db.commit();
    return file;
  }

  private void setUserWithValidPermission(ProjectData privateProject) {
    userSession.logIn("login")
      .addProjectPermission(UserRole.CODEVIEWER, privateProject.getProjectDto())
      .registerBranches(privateProject.getMainBranchDto());
  }

  private ComponentDto insertFile(ComponentDto project) {
    ComponentDto file = newFileDto(project);
    componentDao.insertWithAudit(db.getSession(), file);
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
    dto.setRootComponentUuid(componentDto.uuid());
    snapshotDao.insert(db.getSession(), dto);
  }
}
