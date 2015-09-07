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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoFunctions;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.test.index.CoveredFileDoc;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.user.UserSession;

public class ListAction implements TestsWsAction {
  public static final String TEST_UUID = "testUuid";
  public static final String TEST_FILE_UUID = "testFileUuid";
  public static final String TEST_FILE_KEY = "testFileKey";
  public static final String SOURCE_FILE_UUID = "sourceFileUuid";
  public static final String SOURCE_FILE_LINE_NUMBER = "sourceFileLineNumber";

  private final DbClient dbClient;
  private final TestIndex testIndex;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ListAction(DbClient dbClient, TestIndex testIndex, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.testIndex = testIndex;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("list")
      .setDescription(
        "Get the list of tests.<br /> " +
          "Require Browse permission on file's project.<br /> " +
          "One (and only one) of the following combination of parameters must be provided: " +
          "<ul>" +
          "<li>Test file UUID</li>" +
          "<li>Test UUID</li>" +
          "<li>Source file UUID and Source file line number</li>" +
          "</ul>")
      .setSince("5.2")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-list.json"))
      .setHandler(this)
      .addPagingParams(100);

    action
      .createParam(TEST_FILE_UUID)
      .setDescription("Test file UUID")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action
      .createParam(TEST_FILE_KEY)
      .setDescription("Test file key")
      .setExampleValue("org.codehaus.sonar:sonar-server:src/test/java/org/sonar/server/rule/RubyRuleServiceTest.java");

    action
      .createParam(TEST_UUID)
      .setDescription("Test UUID")
      .setExampleValue("c526ef20-131b-4486-9357-063fa64b5079");

    action
      .createParam(SOURCE_FILE_UUID)
      .setDescription("Source file UUID. Must be provided with the source file line number.")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");

    action
      .createParam(SOURCE_FILE_LINE_NUMBER)
      .setDescription("Source file line number. Must be provided with the source file UUID.")
      .setExampleValue("10");
  }

  @Override
  public void handle(Request request, Response response) {
    String testUuid = request.param(TEST_UUID);
    String testFileUuid = request.param(TEST_FILE_UUID);
    String testFileKey = request.param(TEST_FILE_KEY);
    String sourceFileUuid = request.param(SOURCE_FILE_UUID);
    Integer sourceFileLineNumber = request.paramAsInt(SOURCE_FILE_LINE_NUMBER);
    SearchOptions searchOptions = new SearchOptions().setPage(
      request.mandatoryParamAsInt(WebService.Param.PAGE),
      request.mandatoryParamAsInt(WebService.Param.PAGE_SIZE)
    );

    DbSession dbSession = dbClient.openSession(false);
    SearchResult<TestDoc> tests;
    Map<String, ComponentDto> componentsByTestFileUuid;
    try {
      tests = searchTests(dbSession, testUuid, testFileUuid, testFileKey, sourceFileUuid, sourceFileLineNumber, searchOptions);
      componentsByTestFileUuid = buildComponentsByTestFileUuid(dbSession, tests.getDocs());
    } finally {
      MyBatis.closeQuietly(dbSession);
    }

    JsonWriter json = response.newJsonWriter().beginObject();
    writeTests(tests.getDocs(), componentsByTestFileUuid, json);
    searchOptions.writeJson(json, tests.getTotal());
    json.endObject().close();
  }

  private static void writeTests(List<TestDoc> tests, Map<String, ComponentDto> componentsByTestFileUuid, JsonWriter json) {
    json.name("tests").beginArray();
    for (TestDoc test : tests) {
      String fileUuid = test.fileUuid();
      json.beginObject();
      json.prop("testUuid", test.testUuid());
      json.prop("fileUuid", fileUuid);
      json.prop("name", test.name());
      json.prop("status", test.status());
      json.prop("durationInMs", test.durationInMs());
      json.prop("message", test.message());
      json.prop("stacktrace", test.stackTrace());
      json.prop("coveredLines", coveredLines(test.coveredFiles()));
      json.prop("fileKey", componentsByTestFileUuid.get(fileUuid).key());
      json.prop("fileLongName", componentsByTestFileUuid.get(fileUuid).longName());
      json.endObject();
    }
    json.endArray();
  }

  private static long coveredLines(List<CoveredFileDoc> coveredFiles) {
    long numberOfLinesCovered = 0L;
    for (CoveredFileDoc coveredFile : coveredFiles) {
      numberOfLinesCovered += coveredFile.coveredLines().size();
    }

    return numberOfLinesCovered;
  }

  private Map<String, ComponentDto> buildComponentsByTestFileUuid(DbSession dbSession, List<TestDoc> tests) {
    List<String> fileUuids = Lists.transform(tests, new TestToFileUuidFunction());
    List<ComponentDto> components = dbClient.componentDao().selectByUuids(dbSession, fileUuids);

    return Maps.uniqueIndex(components, ComponentDtoFunctions.toUuid());
  }

  private static class TestToFileUuidFunction implements Function<TestDoc, String> {
    @Override
    public String apply(@Nonnull TestDoc testDoc) {
      return testDoc.fileUuid();
    }
  }

  private SearchResult<TestDoc> searchTests(DbSession dbSession, @Nullable String testUuid, @Nullable String testFileUuid, @Nullable String testFileKey,
                                            @Nullable String sourceFileUuid, @Nullable Integer sourceFileLineNumber, SearchOptions searchOptions) {
    if (testUuid != null) {
      return searchTestsByTestUuid(dbSession, testUuid, searchOptions);
    }
    if (testFileUuid != null) {
      return searchTestsByTestFileUuid(dbSession, testFileUuid, searchOptions);
    }
    if (testFileKey != null) {
      return searchTestsByTestFileKey(dbSession, testFileKey, searchOptions);
    }
    if (sourceFileUuid != null && sourceFileLineNumber != null) {
      return searchTestsBySourceFileUuidAndLineNumber(dbSession, sourceFileUuid, sourceFileLineNumber, searchOptions);
    }

    throw new IllegalArgumentException(
      "One (and only one) of the following combination of parameters must be provided: 1) test UUID. 2) test file UUID. " +
        "3) test file key. 4) source file UUID and source file line number.");
  }

  private SearchResult<TestDoc> searchTestsBySourceFileUuidAndLineNumber(DbSession dbSession, String sourceFileUuid, Integer sourceFileLineNumber, SearchOptions searchOptions) {
    checkComponentUuidPermission(dbSession, sourceFileUuid);
    return testIndex.searchBySourceFileUuidAndLineNumber(sourceFileUuid, sourceFileLineNumber, searchOptions);
  }

  private SearchResult<TestDoc> searchTestsByTestFileKey(DbSession dbSession, String testFileKey, SearchOptions searchOptions) {
    userSession.checkComponentPermission(UserRole.CODEVIEWER, testFileKey);
    ComponentDto testFile = componentFinder.getByKey(dbSession, testFileKey);

    return testIndex.searchByTestFileUuid(testFile.uuid(), searchOptions);
  }

  private SearchResult<TestDoc> searchTestsByTestFileUuid(DbSession dbSession, String testFileUuid, SearchOptions searchOptions) {
    checkComponentUuidPermission(dbSession, testFileUuid);
    return testIndex.searchByTestFileUuid(testFileUuid, searchOptions);
  }

  private SearchResult<TestDoc> searchTestsByTestUuid(DbSession dbSession, String testUuid, SearchOptions searchOptions) {
    checkComponentUuidPermission(dbSession, testIndex.searchByTestUuid(testUuid).fileUuid());
    return testIndex.searchByTestUuid(testUuid, searchOptions);
  }

  private void checkComponentUuidPermission(DbSession dbSession, String componentUuid) {
    ComponentDto component = dbClient.componentDao().selectOrFailByUuid(dbSession, componentUuid);
    userSession.checkProjectUuidPermission(UserRole.CODEVIEWER, component.projectUuid());
  }
}
