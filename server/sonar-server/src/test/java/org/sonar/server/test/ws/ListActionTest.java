/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.test.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsTests;
import org.sonarqube.ws.WsTests.ListResponse;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.protobuf.DbFileSources.Test.TestStatus.OK;
import static org.sonar.server.test.db.TestTesting.newTest;
import static org.sonar.server.test.ws.ListAction.SOURCE_FILE_ID;
import static org.sonar.server.test.ws.ListAction.SOURCE_FILE_KEY;
import static org.sonar.server.test.ws.ListAction.SOURCE_FILE_LINE_NUMBER;
import static org.sonar.server.test.ws.ListAction.TEST_FILE_ID;
import static org.sonar.server.test.ws.ListAction.TEST_FILE_KEY;
import static org.sonar.server.test.ws.ListAction.TEST_ID;

public class ListActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public EsTester es = new EsTester(new TestIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();

  private TestIndex testIndex = new TestIndex(es.client());
  private TestIndexer testIndexer = new TestIndexer(db.getDbClient(), es.client());

  private ComponentDto project;
  private ComponentDto mainFile;
  private ComponentDto testFile;

  private WsActionTester ws = new WsActionTester(new ListAction(dbClient, testIndex, userSessionRule, TestComponentFinder.from(db)));

  @Before
  public void setUp() throws Exception {
    project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    mainFile = db.components().insertComponent(newFileDto(project));
    testFile = db.components().insertComponent(newFileDto(project).setQualifier(UNIT_TEST_FILE));
  }

  @Test
  public void list_tests() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);
    DbFileSources.Test test = newTest(mainFile, 10, 11, 12, 20, 21, 25).setStatus(OK).build();
    insertTests(testFile, test);

    ListResponse request = call(ws.newRequest().setParam(TEST_ID, test.getUuid()));

    assertThat(request.getTestsList()).hasSize(1);
    WsTests.Test result = request.getTests(0);
    assertThat(result.getId()).isEqualTo(test.getUuid());
    assertThat(result.getName()).isEqualTo(test.getName());
    assertThat(result.getStatus()).isEqualTo(WsTests.TestStatus.OK);
    assertThat(result.getFileId()).isEqualTo(testFile.uuid());
    assertThat(result.getFileKey()).isEqualTo(testFile.key());
    assertThat(result.getFileName()).isEqualTo(testFile.path());
    assertThat(result.getDurationInMs()).isEqualTo(test.getExecutionTimeMs());
    assertThat(result.getMessage()).isEqualTo(test.getMsg());
    assertThat(result.getStacktrace()).isEqualTo(test.getStacktrace());
    assertThat(result.getCoveredLines()).isEqualTo(6);
  }

  @Test
  public void list_tests_by_test_uuid() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);
    DbFileSources.Test test1 = newTest(mainFile, 10).build();
    DbFileSources.Test test2 = newTest(mainFile, 11).build();
    insertTests(testFile, test1, test2);

    ListResponse request = call(ws.newRequest().setParam(TEST_ID, test1.getUuid()));

    assertThat(request.getTestsList()).extracting(WsTests.Test::getId).containsOnly(test1.getUuid());
  }

  @Test
  public void list_tests_by_test_file_uuid() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);
    ComponentDto anotherTestFile = db.components().insertComponent(newFileDto(project));
    DbFileSources.Test test1 = newTest(mainFile, 10).build();
    DbFileSources.Test test2 = newTest(mainFile, 11).build();
    DbFileSources.Test test3 = newTest(mainFile, 12).build();
    insertTests(testFile, test1, test2);
    insertTests(anotherTestFile, test3);

    ListResponse request = call(ws.newRequest().setParam(TEST_FILE_ID, testFile.uuid()));

    assertThat(request.getTestsList()).extracting(WsTests.Test::getId).containsOnly(test1.getUuid(), test2.getUuid());
  }

  @Test
  public void list_tests_by_test_file_key() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);
    ComponentDto anotherTestFile = db.components().insertComponent(newFileDto(project));
    DbFileSources.Test test1 = newTest(mainFile, 10).build();
    DbFileSources.Test test2 = newTest(mainFile, 11).build();
    DbFileSources.Test test3 = newTest(mainFile, 12).build();
    insertTests(testFile, test1, test2);
    insertTests(anotherTestFile, test3);

    ListResponse request = call(ws.newRequest().setParam(TEST_FILE_KEY, testFile.key()));

    assertThat(request.getTestsList()).extracting(WsTests.Test::getId).containsOnly(test1.getUuid(), test2.getUuid());
  }

  @Test
  public void list_tests_by_source_file_uuid_and_line_number() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);
    ComponentDto anotherMainFile = db.components().insertComponent(newFileDto(project));
    DbFileSources.Test test1 = newTest(mainFile, 10, 11, 12).build();
    DbFileSources.Test test2 = newTest(mainFile, 9, 11).build();
    DbFileSources.Test test3 = newTest(mainFile, 10, 12).build();
    DbFileSources.Test test4 = newTest(anotherMainFile, 11).build();
    insertTests(testFile, test1, test2, test3, test4);

    ListResponse request = call(ws.newRequest().setParam(SOURCE_FILE_ID, mainFile.uuid()).setParam(SOURCE_FILE_LINE_NUMBER, "11"));

    assertThat(request.getTestsList()).extracting(WsTests.Test::getId).containsOnly(test1.getUuid(), test2.getUuid());
  }

  @Test
  public void list_tests_by_source_file_key_and_line_number() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);
    ComponentDto anotherMainFile = db.components().insertComponent(newFileDto(project));
    DbFileSources.Test test1 = newTest(mainFile, 10, 11, 12).build();
    DbFileSources.Test test2 = newTest(mainFile, 9, 11).build();
    DbFileSources.Test test3 = newTest(mainFile, 10, 12).build();
    DbFileSources.Test test4 = newTest(anotherMainFile, 11).build();
    insertTests(testFile, test1, test2, test3, test4);

    ListResponse request = call(ws.newRequest().setParam(SOURCE_FILE_KEY, mainFile.key()).setParam(SOURCE_FILE_LINE_NUMBER, "10"));

    assertThat(request.getTestsList()).extracting(WsTests.Test::getId).containsOnly(test1.getUuid(), test3.getUuid());
  }

  @Test
  public void tests_are_paginated() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);
    insertTests(testFile, newTest(mainFile, 10).build(), newTest(mainFile, 11).build(), newTest(mainFile, 12).build());

    ListResponse request = call(ws.newRequest().setParam(TEST_FILE_ID, testFile.uuid()));

    assertThat(request.getPaging().getPageIndex()).isEqualTo(1);
    assertThat(request.getPaging().getPageSize()).isEqualTo(100);
    assertThat(request.getPaging().getTotal()).isEqualTo(3);
  }

  @Test
  public void fail_when_no_argument() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);

    expectedException.expect(IllegalArgumentException.class);
    call(ws.newRequest());
  }

  @Test
  public void fail_when_source_file_uuid_without_line_number() throws Exception {
    userSessionRule.addProjectPermission(CODEVIEWER, project);

    expectedException.expect(IllegalArgumentException.class);
    call(ws.newRequest().setParam(SOURCE_FILE_ID, mainFile.uuid()));
  }

  @Test
  public void fail_when_not_enough_privilege_on_test_uuid() throws Exception {
    userSessionRule.addProjectPermission(USER, project);
    DbFileSources.Test test = newTest(mainFile, 10).build();
    insertTests(testFile, test);

    expectedException.expect(ForbiddenException.class);
    call(ws.newRequest().setParam(TEST_ID, test.getUuid()));
  }

  @Test
  public void fail_when_no_enough_privilege_on_test_file_id() throws Exception {
    userSessionRule.addProjectPermission(USER, project);
    insertTests(testFile, newTest(mainFile, 10).build());

    expectedException.expect(ForbiddenException.class);
    call(ws.newRequest().setParam(TEST_FILE_ID, testFile.uuid()));
  }

  @Test
  public void fail_when_not_enough_privilege_on_test_file_key() throws Exception {
    userSessionRule.addProjectPermission(USER, project);
    insertTests(testFile, newTest(mainFile, 10).build());

    expectedException.expect(ForbiddenException.class);
    call(ws.newRequest().setParam(TEST_FILE_KEY, testFile.key()));
  }

  @Test
  public void fail_when_not_enough_privilege_on_main_file_uuid() throws Exception {
    userSessionRule.addProjectPermission(USER, project);
    insertTests(testFile, newTest(mainFile, 10).build());

    expectedException.expect(ForbiddenException.class);
    call(ws.newRequest().setParam(SOURCE_FILE_ID, mainFile.uuid()).setParam(SOURCE_FILE_LINE_NUMBER, "10"));
  }

  @Test
  public void fail_when_test_uuid_is_unknown() throws Exception {
    expectedException.expect(NotFoundException.class);
    call(ws.newRequest().setParam(TEST_ID, "unknown"));
  }

  @Test
  public void fail_when_test_file_id_is_unknown() throws Exception {
    expectedException.expect(NotFoundException.class);
    call(ws.newRequest().setParam(TEST_FILE_ID, "unknown"));
  }

  @Test
  public void fail_when_test_file_key_is_unknown() throws Exception {
    expectedException.expect(NotFoundException.class);
    call(ws.newRequest().setParam(TEST_FILE_ID, "unknown"));
  }

  @Test
  public void fail_when_source_file_id_is_unknown() throws Exception {
    expectedException.expect(NotFoundException.class);
    call(ws.newRequest().setParam(SOURCE_FILE_ID, "unknown").setParam(SOURCE_FILE_LINE_NUMBER, "10"));
  }

  @Test
  public void fail_when_source_file_key_is_unknown() throws Exception {
    expectedException.expect(NotFoundException.class);
    call(ws.newRequest().setParam(SOURCE_FILE_KEY, "unknown").setParam(SOURCE_FILE_LINE_NUMBER, "10"));
  }

  private void insertTests(ComponentDto testFile, DbFileSources.Test... tests) {
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(project.uuid())
      .setFileUuid(testFile.uuid())
      .setTestData(asList(tests)));
    db.commit();
    testIndexer.indexOnStartup(null);
  }

  private static ListResponse call(TestRequest request) {
    return request
      .executeProtobuf(ListResponse.class);
  }

}
