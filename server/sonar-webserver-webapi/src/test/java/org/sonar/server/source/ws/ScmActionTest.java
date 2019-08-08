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

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static java.lang.String.format;

public class ScmActionTest {

  private static final String FILE_KEY = "FILE_KEY";
  private static final String FILE_UUID = "FILE_A";
  private static final String PROJECT_UUID = "PROJECT_A";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private WsTester tester;
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private ComponentDto project;
  private ComponentDto file;

  @Before
  public void setUp() {
    tester = new WsTester(
      new SourcesWs(new ScmAction(dbClient, new SourceService(dbTester.getDbClient(), new HtmlSourceDecorator()), userSessionRule, TestComponentFinder.from(dbTester))));

    project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert(), PROJECT_UUID);
    file = ComponentTesting.newFileDto(project, null, FILE_UUID).setDbKey(FILE_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), project, file);
    dbTester.getSession().commit();
  }

  @Test
  public void show_scm() throws Exception {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder().addLines(
        newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1)).build()));
    dbSession.commit();

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY);
    request.execute().assertJson(getClass(), "show_scm.json");
  }

  @Test
  public void show_scm_from_given_range_lines() throws Exception {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("from", "2").setParam("to", "3");
    request.execute().assertJson(getClass(), "show_scm_from_given_range_lines.json");
  }

  @Test
  public void not_group_lines_by_commit() throws Exception {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    // lines 1 and 2 are the same commit, but not 3 (different date)
    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("commits_by_line",
      "true");
    request.execute().assertJson(getClass(), "not_group_lines_by_commit.json");
  }

  @Test
  public void group_lines_by_commit() throws Exception {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    // lines 1 and 2 are the same commit, but not 3 (different date)
    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("commits_by_line",
      "false");
    request.execute().assertJson(getClass(), "group_lines_by_commit.json");
  }

  @Test
  public void accept_negative_value_in_from_parameter() throws Exception {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder()
        .addLines(newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1))
        .addLines(newSourceLine("julien", "123-456-710", DateUtils.parseDateTime("2015-03-29T12:34:56+0000"), 2))
        .addLines(newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3))
        .addLines(newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4))
        .build()));
    dbSession.commit();

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("from",
      "-2").setParam("to", "3");
    request.execute().assertJson(getClass(), "accept_negative_value_in_from_parameter.json");
  }

  @Test
  public void return_empty_value_when_no_scm() throws Exception {
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    dbTester.getDbClient().fileSourceDao().insert(dbSession, new FileSourceDto()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setSourceData(DbFileSources.Data.newBuilder().build()));
    dbSession.commit();

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY);
    request.execute().assertJson(getClass(), "return_empty_value_when_no_scm.json");
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_code_viewer_permission() throws Exception {
    userSessionRule.addProjectPermission(UserRole.USER, project, file);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY);
    request.execute();
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = dbTester.components().insertMainBranch();
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    tester.newGetRequest("api/sources", "scm")
      .setParam("key", branch.getDbKey())
      .execute();
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
