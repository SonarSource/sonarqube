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

import java.util.Date;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

public class ScmActionTest {

  private static final String FILE_KEY = "FILE_KEY";
  private static final String FILE_UUID = "FILE_A";
  private static final String PROJECT_UUID = "PROJECT_A";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WsTester tester;

  DbClient dbClient = dbTester.getDbClient();

  @Before
  public void setUp() {
    dbTester.truncateTables();
    esTester.truncateIndices();
    tester = new WsTester(new SourcesWs(new ScmAction(dbClient, new SourceLineIndex(esTester.client()), userSessionRule, new ComponentFinder(dbClient))));
  }

  @Test
  public void show_scm() throws Exception {
    initFile();
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1)
    );

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY);
    request.execute().assertJson(getClass(), "show_scm.json");
  }

  @Test
  public void show_scm_from_given_range_lines() throws Exception {
    initFile();
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1),
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2),
      newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3),
      newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4)
    );

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("from", "2").setParam("to", "3");
    request.execute().assertJson(getClass(), "show_scm_from_given_range_lines.json");
  }

  @Test
  public void not_group_lines_by_commit() throws Exception {
    initFile();
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    // lines 1 and 2 are the same commit, but not 3 (different date)
    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1),
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2),
      newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3),
      newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4)
    );

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("commits_by_line", "true");
    request.execute().assertJson(getClass(), "not_group_lines_by_commit.json");
  }

  @Test
  public void group_lines_by_commit() throws Exception {
    initFile();
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    // lines 1 and 2 are the same commit, but not 3 (different date)
    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1),
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 2),
      newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3),
      newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4)
    );

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("commits_by_line", "false");
    request.execute().assertJson(getClass(), "group_lines_by_commit.json");
  }

  @Test
  public void accept_negative_value_in_from_parameter() throws Exception {
    initFile();
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      newSourceLine("julien", "123-456-789", DateUtils.parseDateTime("2015-03-30T12:34:56+0000"), 1),
      newSourceLine("julien", "123-456-710", DateUtils.parseDateTime("2015-03-29T12:34:56+0000"), 2),
      newSourceLine("julien", "456-789-101", DateUtils.parseDateTime("2015-03-27T12:34:56+0000"), 3),
      newSourceLine("simon", "789-101-112", DateUtils.parseDateTime("2015-03-31T12:34:56+0000"), 4)
    );

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY).setParam("from", "-2").setParam("to", "3");
    request.execute().assertJson(getClass(), "accept_negative_value_in_from_parameter.json");
  }

  @Test
  public void return_empty_value_when_no_scm() throws Exception {
    initFile();
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      newSourceLine(null, null, null, 1)
    );

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY);
    request.execute().assertJson(getClass(), "return_empty_value_when_no_scm.json");
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_code_viewer_permission() throws Exception {
    initFile();
    userSessionRule.addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "scm").setParam("key", FILE_KEY);
    request.execute();
  }

  private void initFile() {
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID);
    dbClient.componentDao().insert(dbTester.getSession(), project, ComponentTesting.newFileDto(project, FILE_UUID).setKey(FILE_KEY));
    dbTester.getSession().commit();
  }

  private SourceLineDoc newSourceLine(String author, String revision, Date date, int line) {
    return new SourceLineDoc()
      .setScmAuthor(author)
      .setScmRevision(revision)
      .setScmDate(date)
      .setLine(line)
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID);
  }
}
