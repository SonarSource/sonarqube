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

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.fail;

public class LinesActionTest {

  private static final String PROJECT_UUID = "abcd";
  private static final String FILE_UUID = "efgh";
  private static final String FILE_KEY = "Foo.java";

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  @ClassRule
  public static DbTester dbTester = new DbTester();

  SourceLineIndex sourceLineIndex;

  HtmlSourceDecorator htmlSourceDecorator;

  ComponentDao componentDao;

  DbSession session;

  WsTester wsTester;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    esTester.truncateIndices();

    htmlSourceDecorator = new HtmlSourceDecorator();
    sourceLineIndex = new SourceLineIndex(esTester.client());
    componentDao = new ComponentDao();
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), componentDao);
    session = dbClient.openSession(false);
    wsTester = new WsTester(new SourcesWs(new LinesAction(dbClient, sourceLineIndex, htmlSourceDecorator)));

    MockUserSession.set();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void show_source() throws Exception {
    newFile();

    Date updatedAt = new Date();
    String scmDate = "2014-01-01T12:34:56.789Z";
    SourceLineDoc line1 = new SourceLineDoc()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setLine(1)
      .setScmRevision("cafebabe")
      .setScmAuthor("polop")
      .setSource("package org.polop;")
      .setHighlighting("0,7,k")
      .setSymbols("8,17,42")
      .setUtLineHits(3)
      .setUtConditions(2)
      .setUtCoveredConditions(1)
      .setItLineHits(3)
      .setItConditions(2)
      .setItCoveredConditions(1)
      .setDuplications(ImmutableList.<Integer>of())
      .setUpdateDate(updatedAt);
    line1.setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);

    SourceLineDoc line2 = new SourceLineDoc()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setLine(2)
      .setScmRevision("cafebabe")
      .setScmAuthor("polop")
      .setSource("abc")
      .setHighlighting("0,5,c")
      .setSymbols("")
      .setUtLineHits(3)
      .setUtConditions(2)
      .setUtCoveredConditions(1)
      .setItLineHits(null)
      .setItConditions(null)
      .setItCoveredConditions(null)
      .setDuplications(ImmutableList.of(1))
      .setUpdateDate(updatedAt);
    line2.setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);

    SourceLineDoc line3 = new SourceLineDoc()
      .setProjectUuid(PROJECT_UUID)
      .setFileUuid(FILE_UUID)
      .setLine(3)
      .setScmRevision("cafebabe")
      .setScmAuthor("polop")
      .setSource("}")
      .setHighlighting(null)
      .setSymbols(null)
      .setUtLineHits(null)
      .setUtConditions(null)
      .setUtCoveredConditions(null)
      .setItLineHits(3)
      .setItConditions(2)
      .setItCoveredConditions(1)
      .setDuplications(ImmutableList.<Integer>of())
      .setUpdateDate(updatedAt);
    line3.setField(SourceLineIndexDefinition.FIELD_SCM_DATE, scmDate);

    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE, line1, line2, line3);

    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", FILE_UUID);
    request.execute().assertJson(getClass(), "show_source.json");
  }

  @Test
  public void fail_to_show_source_if_no_source_found() throws Exception {
    newFile();

    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    try {
      WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("uuid", FILE_UUID);
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void show_source_with_from_and_to_params() throws Exception {
    newFile();

    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      new SourceLineDoc()
        .setProjectUuid(PROJECT_UUID)
        .setFileUuid(FILE_UUID)
        .setLine(3)
        .setScmRevision("cafebabe")
        .setScmDate(null)
        .setScmAuthor("polop")
        .setSource("}")
        .setHighlighting("")
        .setSymbols("")
        .setUtLineHits(null)
        .setUtConditions(null)
        .setUtCoveredConditions(null)
        .setItLineHits(null)
        .setItConditions(null)
        .setItCoveredConditions(null)
        .setDuplications(null)
        .setUpdateDate(new Date())
    );

    WsTester.TestRequest request = wsTester
      .newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID)
      .setParam("from", "3")
      .setParam("to", "3");
    request.execute().assertJson(getClass(), "show_source_with_params_from_and_to.json");
  }

  @Test
  public void show_source_by_file_key() throws Exception {
    newFile();

    esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      new SourceLineDoc()
        .setProjectUuid(PROJECT_UUID)
        .setFileUuid(FILE_UUID)
        .setLine(3)
        .setScmRevision("cafebabe")
        .setScmDate(null)
        .setScmAuthor("polop")
        .setSource("}")
        .setHighlighting("")
        .setSymbols("")
        .setUtLineHits(null)
        .setUtConditions(null)
        .setUtCoveredConditions(null)
        .setItLineHits(null)
        .setItConditions(null)
        .setItCoveredConditions(null)
        .setDuplications(null)
        .setUpdateDate(new Date())
    );

    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);

    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines").setParam("key", FILE_KEY);
    request.execute().assertJson(getClass(), "show_source_by_file_key.json");
  }

  @Test
  public void fail_when_no_uuid_or_key_param() throws Exception {
    newFile();
    MockUserSession.set().setLogin("login").addProjectUuidPermissions(UserRole.CODEVIEWER, PROJECT_UUID);
    WsTester.TestRequest request = wsTester.newGetRequest("api/sources", "lines");

    try {
      request.execute();
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Param uuid or param key is missing");
    }
  }

  @Test(expected = ForbiddenException.class)
  public void should_check_permission() throws Exception {
    newFile();

    MockUserSession.set().setLogin("login");

    wsTester.newGetRequest("api/sources", "lines")
      .setParam("uuid", FILE_UUID)
      .execute();
  }

  private void newFile(){
    ComponentDto project = ComponentTesting.newProjectDto(PROJECT_UUID);
    ComponentDto file = ComponentTesting.newFileDto(project, FILE_UUID).setKey(FILE_KEY);
    componentDao.insert(session, project, file);
    session.commit();
  }
}
