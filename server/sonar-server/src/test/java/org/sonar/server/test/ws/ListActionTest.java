/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.test.ws;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.test.index.CoveredFileDoc;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

public class ListActionTest {

  @Rule
  public EsTester es = new EsTester(new TestIndexDefinition(new Settings()));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();

  TestIndex testIndex;
  WsTester ws;

  @Before
  public void setUp() {
    testIndex = new TestIndex(es.client());
    ws = new WsTester(new TestsWs(new ListAction(dbClient, testIndex, userSessionRule, new ComponentFinder(dbClient))));
  }

  @Test
  public void list_based_on_test_uuid() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, TestFile1.PROJECT_UUID);

    dbClient.componentDao().insert(db.getSession(), TestFile1.dto());
    db.getSession().commit();

    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE, TestFile1.doc());

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list").setParam(ListAction.TEST_ID, TestFile1.UUID);

    request.execute().assertJson(getClass(), "list-test-uuid.json");
  }

  @Test
  public void list_based_on_test_file_uuid() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, TestFile1.PROJECT_UUID);
    dbClient.componentDao().insert(db.getSession(), TestFile1.dto());
    db.getSession().commit();

    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE, TestFile1.doc());

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list").setParam(ListAction.TEST_FILE_ID, TestFile1.FILE_UUID);

    request.execute().assertJson(getClass(), "list-test-uuid.json");
  }

  @Test
  public void list_based_on_test_file_key() throws Exception {
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, TestFile1.PROJECT_UUID, TestFile1.KEY);
    dbClient.componentDao().insert(db.getSession(), TestFile1.dto());
    db.getSession().commit();

    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE, TestFile1.doc());

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list").setParam(ListAction.TEST_FILE_KEY, TestFile1.KEY);

    request.execute().assertJson(getClass(), "list-test-uuid.json");
  }

  @Test
  public void list_based_on_source_file_uuid_and_line_number() throws Exception {
    String mainFileUuid = "MAIN-FILE-UUID";
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, TestFile1.PROJECT_UUID);
    dbClient.componentDao().insert(db.getSession(),
      TestFile1.dto(),
      TestFile2.dto(),
      new ComponentDto()
        .setUuid(mainFileUuid)
        .setUuidPath(TestFile1.PROJECT_UUID + "." + mainFileUuid + ".")
        .setRootUuid(TestFile1.PROJECT_UUID)
        .setProjectUuid(TestFile1.PROJECT_UUID));
    db.getSession().commit();

    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      TestFile1.doc(),
      TestFile2.doc());

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list")
      .setParam(ListAction.SOURCE_FILE_ID, mainFileUuid)
      .setParam(ListAction.SOURCE_FILE_LINE_NUMBER, "10");

    request.execute().assertJson(getClass(), "list-main-file.json");
  }

  @Test
  public void list_based_on_source_file_key_and_line_number() throws Exception {
    String sourceFileUuid = "MAIN-FILE-UUID";
    String sourceFileKey = "MAIN-FILE-KEY";
    userSessionRule.addProjectUuidPermissions(UserRole.CODEVIEWER, TestFile1.PROJECT_UUID);
    dbClient.componentDao().insert(db.getSession(),
      TestFile1.dto(),
      TestFile2.dto(),
      new ComponentDto()
        .setUuid(sourceFileUuid)
        .setUuidPath(TestFile1.PROJECT_UUID + "." + sourceFileUuid + ".")
        .setRootUuid(TestFile1.PROJECT_UUID)
        .setKey(sourceFileKey)
        .setProjectUuid(TestFile1.PROJECT_UUID));
    db.getSession().commit();

    es.putDocuments(TestIndexDefinition.INDEX, TestIndexDefinition.TYPE,
      TestFile1.doc(), TestFile2.doc());

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list")
      .setParam(ListAction.SOURCE_FILE_KEY, sourceFileKey)
      .setParam(ListAction.SOURCE_FILE_LINE_NUMBER, "10");

    request.execute().assertJson(getClass(), "list-main-file.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_when_no_argument() throws Exception {
    ws.newGetRequest("api/tests", "list").execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_when_main_file_uuid_without_line_number() throws Exception {
    ws.newGetRequest("api/tests", "list").setParam(ListAction.SOURCE_FILE_ID, "ANY-UUID").execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_when_no_sufficent_privilege_on_file_uuid() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.USER, TestFile1.PROJECT_UUID);
    dbClient.componentDao().insert(db.getSession(), TestFile1.dto());
    db.getSession().commit();
    ws.newGetRequest("api/tests", "list").setParam(ListAction.TEST_FILE_ID, TestFile1.FILE_UUID).execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_when_no_sufficent_privilege_on_test_uuid() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.USER, TestFile1.PROJECT_UUID);
    dbClient.componentDao().insert(db.getSession(), TestFile1.dto());
    db.getSession().commit();
    ws.newGetRequest("api/tests", "list").setParam(ListAction.TEST_FILE_ID, TestFile1.FILE_UUID).execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_when_no_sufficent_privilege_on_file_key() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.USER, TestFile1.PROJECT_UUID);
    dbClient.componentDao().insert(db.getSession(), TestFile1.dto());
    db.getSession().commit();
    ws.newGetRequest("api/tests", "list").setParam(ListAction.TEST_FILE_KEY, TestFile1.KEY).execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_when_no_sufficient_privilege_on_main_file_uuid() throws Exception {
    userSessionRule.addProjectUuidPermissions(UserRole.USER, TestFile1.PROJECT_UUID);
    String mainFileUuid = "MAIN-FILE-UUID";
    dbClient.componentDao().insert(db.getSession(), new ComponentDto()
      .setUuid(mainFileUuid)
      .setUuidPath(TestFile1.PROJECT_UUID + "." + mainFileUuid + ".")
      .setRootUuid(TestFile1.PROJECT_UUID)
      .setProjectUuid(TestFile1.PROJECT_UUID));
    db.getSession().commit();

    ws.newGetRequest("api/tests", "list")
      .setParam(ListAction.SOURCE_FILE_ID, mainFileUuid)
      .setParam(ListAction.SOURCE_FILE_LINE_NUMBER, "10")
      .execute();
  }

  @Test
  public void fail_when_test_uuid_is_unknown() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Test with id 'unknown-test-uuid' is not found");

    ws.newGetRequest("api/tests", "list")
      .setParam(ListAction.TEST_ID, "unknown-test-uuid")
      .execute();
  }

  private static final class TestFile1 {
    public static final String UUID = "TEST-UUID-1";
    public static final String FILE_UUID = "ABCD";
    public static final String FILE_UUID_PATH = "PROJECT-UUID.ABCD.";
    public static final String PROJECT_UUID = "PROJECT-UUID";
    public static final String NAME = "test1";
    public static final String STATUS = "OK";
    public static final long DURATION_IN_MS = 10;
    public static final String MESSAGE = "MESSAGE-1";
    public static final String STACKTRACE = "STACKTRACE-1";
    public static final String KEY = "org.foo.BarTest.java";
    public static final String LONG_NAME = "src/test/java/org/foo/BarTest.java";
    public static final List<CoveredFileDoc> COVERED_FILES = Arrays.asList(new CoveredFileDoc().setFileUuid("MAIN-FILE-UUID").setCoveredLines(Arrays.asList(1, 2, 3, 10)));

    public static ComponentDto dto() {
      return new ComponentDto()
        .setUuid(TestFile1.FILE_UUID)
        .setUuidPath(TestFile1.FILE_UUID_PATH)
        .setRootUuid(TestFile1.PROJECT_UUID)
        .setLongName(TestFile1.LONG_NAME)
        .setProjectUuid(TestFile1.PROJECT_UUID)
        .setKey(TestFile1.KEY);
    }

    public static TestDoc doc() {
      return new TestDoc()
        .setUuid(TestFile1.UUID)
        .setProjectUuid(TestFile1.PROJECT_UUID)
        .setName(TestFile1.NAME)
        .setFileUuid(TestFile1.FILE_UUID)
        .setDurationInMs(TestFile1.DURATION_IN_MS)
        .setStatus(TestFile1.STATUS)
        .setMessage(TestFile1.MESSAGE)
        .setCoveredFiles(TestFile1.COVERED_FILES)
        .setStackTrace(TestFile1.STACKTRACE);
    }

    private TestFile1() {
      // static stuff for test purposes
    }
  }

  private static final class TestFile2 {
    public static final String UUID = "TEST-UUID-2";
    public static final String FILE_UUID = "BCDE";
    public static final String FILE_UUID_PATH = "PROJECT-UUID.BCDE.";
    public static final String PROJECT_UUID = "PROJECT-UUID";
    public static final String NAME = "test2";
    public static final String STATUS = "ERROR";
    public static final long DURATION_IN_MS = 97;
    public static final String MESSAGE = "MESSAGE-2";
    public static final String STACKTRACE = "STACKTRACE-2";
    public static final String KEY = "org.foo.FileTest.java";
    public static final String LONG_NAME = "src/test/java/org/foo/FileTest.java";
    public static final List<CoveredFileDoc> COVERED_FILES = Arrays.asList(new CoveredFileDoc().setFileUuid("MAIN-FILE-UUID").setCoveredLines(Arrays.asList(11, 12, 13, 10)));

    public static ComponentDto dto() {
      return new ComponentDto()
        .setUuid(FILE_UUID)
        .setUuidPath(FILE_UUID_PATH)
        .setRootUuid(TestFile2.PROJECT_UUID)
        .setLongName(TestFile2.LONG_NAME)
        .setProjectUuid(TestFile2.PROJECT_UUID)
        .setKey(TestFile2.KEY);
    }

    public static TestDoc doc() {
      return new TestDoc()
        .setUuid(TestFile2.UUID)
        .setProjectUuid(TestFile2.PROJECT_UUID)
        .setName(TestFile2.NAME)
        .setFileUuid(TestFile2.FILE_UUID)
        .setDurationInMs(TestFile2.DURATION_IN_MS)
        .setStatus(TestFile2.STATUS)
        .setStackTrace(TestFile2.STATUS)
        .setMessage(TestFile2.MESSAGE)
        .setCoveredFiles(TestFile2.COVERED_FILES)
        .setStackTrace(TestFile2.STACKTRACE);
    }

    private TestFile2() {
      // static stuff for test purposes
    }
  }

}
