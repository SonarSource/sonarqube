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
package org.sonar.server.source.ws;

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ScmActionIT {

  private static final String FILE_KEY = "FILE_KEY";
  private static final String FILE_UUID = "FILE_A";
  private static final String PROJECT_UUID = "PROJECT_A";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final ScmAction underTest = new ScmAction(dbClient, new SourceService(dbTester.getDbClient(), new HtmlSourceDecorator()),
    userSessionRule, TestComponentFinder.from(dbTester));
  private final WsActionTester tester = new WsActionTester(underTest);
  private ProjectData project;
  private ComponentDto file;

  @Before
  public void setUp() {
    project = dbTester.components().insertPrivateProject(PROJECT_UUID);
    file = ComponentTesting.newFileDto(project.getMainBranchComponent(), null, FILE_UUID).setKey(FILE_KEY);
    dbClient.componentDao().insertWithAudit(dbTester.getSession(), file);
    dbTester.getSession().commit();
  }

  @Test
  public void show_scm() {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder().addLines(
        newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1)).build()));
    dbSession.commit();

    tester.newRequest()
      .setParam("key", FILE_KEY)
      .execute()
      .assertJson(getClass(), "show_scm.json");
  }

  @Test
  public void show_scm_from_given_range_lines() {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    tester.newRequest()
      .setParam("key", FILE_KEY)
      .setParam("from", "2")
      .setParam("to", "3")
      .execute()
      .assertJson(getClass(), "show_scm_from_given_range_lines.json");
  }

  @Test
  public void not_group_lines_by_commit() {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    // lines 1 and 2 are the same commit, but not 3 (different date)
    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    tester.newRequest()
      .setParam("key", FILE_KEY)
      .setParam("commits_by_line", "true")
      .execute()
      .assertJson(getClass(), "not_group_lines_by_commit.json");
  }

  @Test
  public void group_lines_by_commit() {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    // lines 1 and 2 are the same commit, but not 3 (different date)
    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    tester.newRequest()
      .setParam("key", FILE_KEY)
      .setParam("commits_by_line", "false")
      .execute()
      .assertJson(getClass(), "group_lines_by_commit.json");
  }

  @Test
  public void accept_negative_value_in_from_parameter() {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-710", DateUtils.parseDateTime("2015-03-29T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    tester.newRequest()
      .setParam("key", FILE_KEY)
      .setParam("from", "-2")
      .setParam("to", "3")
      .execute()
      .assertJson(getClass(), "accept_negative_value_in_from_parameter.json");
  }

  @Test
  public void return_empty_value_when_no_scm() {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder().build()));
    dbSession.commit();

    tester.newRequest()
      .setParam("key", FILE_KEY)
      .execute()
      .assertJson(getClass(), "return_empty_value_when_no_scm.json");
  }

  @Test
  public void fail_without_code_viewer_permission() {
    userSessionRule.addProjectPermission(UserRole.USER, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("key", FILE_KEY)
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  private DbFileSources.Line newSourceLine(String author, String revision, Date date, int line) {
    return DbFileSources.Line.newBuilder()
      .setScmAuthor(author)
      .setScmRevision(revision)
      .setScmDate(date.getTime())
      .setLine(line)
      .build();
  }
}
