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

package org.sonar.server.test.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestsListActionTest {
  DbClient dbClient;
  TestIndex testIndex;

  WsTester ws;

  @Before
  public void setUp() throws Exception {
    dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
    testIndex = mock(TestIndex.class);
    ws = new WsTester(new TestsWs(new TestsListAction(dbClient, testIndex)));
  }

  @Test
  public void list_based_on_test_uuid() throws Exception {
    MockUserSession.set().addComponentUuidPermission(UserRole.CODEVIEWER, "SonarQube", "ABCD");
    when(dbClient.componentDao().getByUuids(any(DbSession.class), anyCollectionOf(String.class))).thenReturn(Arrays.asList(
      new ComponentDto()
        .setUuid(File1.FILE_UUID)
        .setLongName(File1.LONG_NAME)
        .setKey(File1.KEY)
      ));
    when(testIndex.searchByTestUuid(File1.UUID)).thenReturn(new TestDoc()
      .setUuid(File1.UUID)
      .setName(File1.NAME)
      .setFileUuid(File1.FILE_UUID)
      .setDurationInMs(File1.DURATION_IN_MS)
      .setStatus(File1.STATUS)
      .setMessage(File1.MESSAGE)
      .setStackTrace(File1.STACKTRACE)
      );

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list").setParam(TestsListAction.TEST_UUID, File1.UUID);

    request.execute().assertJson(getClass(), "list-test-uuid.json");
  }

  @Test
  public void list_based_on_test_file_uuid() throws Exception {
    MockUserSession.set().addComponentUuidPermission(UserRole.CODEVIEWER, "SonarQube", File1.FILE_UUID);
    when(dbClient.componentDao().getByUuids(any(DbSession.class), anyCollectionOf(String.class))).thenReturn(Arrays.asList(
      new ComponentDto()
        .setUuid(File1.FILE_UUID)
        .setLongName(File1.LONG_NAME)
        .setKey(File1.KEY)
      ));
    when(testIndex.searchByTestFileUuid(File1.FILE_UUID)).thenReturn(
      Arrays.asList(
        new TestDoc()
          .setUuid(File1.UUID)
          .setName(File1.NAME)
          .setFileUuid(File1.FILE_UUID)
          .setDurationInMs(File1.DURATION_IN_MS)
          .setStatus(File1.STATUS)
          .setMessage(File1.MESSAGE)
          .setStackTrace(File1.STACKTRACE)
        )
      );

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list").setParam(TestsListAction.TEST_FILE_UUID, File1.FILE_UUID);

    request.execute().assertJson(getClass(), "list-test-uuid.json");
  }

  @Test
  public void list_based_on_main_file_and_line_number() throws Exception {
    String mainFileUuid = "MAIN-FILE-UUID";
    MockUserSession.set().addComponentUuidPermission(UserRole.CODEVIEWER, "SonarQube", mainFileUuid);
    when(dbClient.componentDao().getByUuids(any(DbSession.class), anyCollectionOf(String.class))).thenReturn(Arrays.asList(
      new ComponentDto()
        .setUuid(File1.FILE_UUID)
        .setLongName(File1.LONG_NAME)
        .setKey(File1.KEY),
      new ComponentDto()
        .setUuid(File2.FILE_UUID)
        .setLongName(File2.LONG_NAME)
        .setKey(File2.KEY)
      ));
    when(testIndex.searchBySourceFileUuidAndLineNumber(mainFileUuid, 10)).thenReturn(
      Arrays.asList(
        new TestDoc()
          .setUuid(File1.UUID)
          .setName(File1.NAME)
          .setFileUuid(File1.FILE_UUID)
          .setDurationInMs(File1.DURATION_IN_MS)
          .setStatus(File1.STATUS)
          .setMessage(File1.MESSAGE)
          .setStackTrace(File1.STACKTRACE),
        new TestDoc()
          .setUuid(File2.UUID)
          .setName(File2.NAME)
          .setFileUuid(File2.FILE_UUID)
          .setDurationInMs(File2.DURATION_IN_MS)
          .setStatus(File2.STATUS)
          .setStackTrace(File2.STATUS)
          .setMessage(File2.MESSAGE)
          .setStackTrace(File2.STACKTRACE)
        )
      );

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "list")
      .setParam(TestsListAction.SOURCE_FILE_UUID, mainFileUuid)
      .setParam(TestsListAction.SOURCE_FILE_LINE_NUMBER, "10");

    request.execute().assertJson(getClass(), "list-main-file.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_when_no_argument() throws Exception {
    ws.newGetRequest("api/tests", "list").execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_when_main_file_uuid_without_line_number() throws Exception {
    ws.newGetRequest("api/tests", "list").setParam(TestsListAction.SOURCE_FILE_UUID, "ANY-UUID").execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_when_no_sufficent_privilege_on_file_uuid() throws Exception {
    MockUserSession.set().addComponentUuidPermission(UserRole.USER, "SonarQube", File1.FILE_UUID);
    ws.newGetRequest("api/tests", "list").setParam(TestsListAction.TEST_FILE_UUID, File1.FILE_UUID).execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_when_no_sufficent_privilege_on_test_uuid() throws Exception {
    MockUserSession.set().addComponentUuidPermission(UserRole.USER, "SonarQube", File1.FILE_UUID);
    ws.newGetRequest("api/tests", "list").setParam(TestsListAction.TEST_FILE_UUID, File1.FILE_UUID).execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_when_no_sufficient_privilege_on_main_file_uuid() throws Exception {
    String mainFileUuid = "MAIN-FILE-UUID";
    MockUserSession.set().addComponentUuidPermission(UserRole.USER, "SonarQube", mainFileUuid);

    ws.newGetRequest("api/tests", "list")
      .setParam(TestsListAction.SOURCE_FILE_UUID, mainFileUuid)
      .setParam(TestsListAction.SOURCE_FILE_LINE_NUMBER, "10")
      .execute();
  }

  private static final class File1 {
    private static final String UUID = "TEST-UUID-1";
    public static final String FILE_UUID = "ABCD";
    public static final String NAME = "test1";
    public static final String STATUS = "OK";
    public static final long DURATION_IN_MS = 10;
    public static final String MESSAGE = "MESSAGE-1";
    public static final String STACKTRACE = "STACKTRACE-1";
    private static final String KEY = "org.foo.BarTest.java";
    public static final String LONG_NAME = "src/test/java/org/foo/BarTest.java";

    private File1() {
      // static stuff for test purposes
    }
  }

  private static final class File2 {
    private static final String UUID = "TEST-UUID-2";
    public static final String FILE_UUID = "BCDE";
    public static final String NAME = "test2";
    public static final String STATUS = "ERROR";
    public static final long DURATION_IN_MS = 97;
    public static final String MESSAGE = "MESSAGE-2";
    public static final String STACKTRACE = "STACKTRACE-2";
    private static final String KEY = "org.foo.FileTest.java";
    public static final String LONG_NAME = "src/test/java/org/foo/FileTest.java";

    private File2() {
      // static stuff for test purposes
    }
  }

}
