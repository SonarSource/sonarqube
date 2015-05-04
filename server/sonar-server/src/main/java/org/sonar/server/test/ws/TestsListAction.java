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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.db.DbClient;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.elasticsearch.common.base.MoreObjects.firstNonNull;

public class TestsListAction implements TestAction {
  public static final String TEST_UUID = "testUuid";
  public static final String TEST_FILE_UUID = "testFileUuid";
  public static final String SOURCE_FILE_UUID = "sourceFileUuid";
  public static final String SOURCE_FILE_LINE_NUMBER = "sourceFileLineNumber";

  private final DbClient dbClient;
  private final TestIndex testIndex;

  public TestsListAction(DbClient dbClient, TestIndex testIndex) {
    this.dbClient = dbClient;
    this.testIndex = testIndex;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("list")
      .setDescription("Get the list of tests.<br /> " +
        "Require Browse permission on file's project.<br /> " +
        "One of the following combination of fields must be filled: " +
        "<ul>" +
        "<li>Test file UUID</li>" +
        "<li>Test UUID</li>" +
        "<li>Source file UUID and Source file line number</li>" +
        "</ul>")
      .setSince("5.2")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-list.json"))
      .setHandler(this);

    action
      .createParam(TEST_FILE_UUID)
      .setDescription("Test file UUID")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action
      .createParam(TEST_UUID)
      .setDescription("Test UUID")
      .setExampleValue("c526ef20-131b-4486-9357-063fa64b5079");

    action
      .createParam(SOURCE_FILE_UUID)
      .setDescription("Source file UUID")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");

    action
      .createParam(SOURCE_FILE_LINE_NUMBER)
      .setDescription("Source file line number")
      .setExampleValue("10");
  }

  @Override
  public void handle(Request request, Response response) {
    String testUuid = request.param(TEST_UUID);
    String testFileUuid = request.param(TEST_FILE_UUID);
    String sourceFileUuid = request.param(SOURCE_FILE_UUID);
    Integer sourceFileLineNumber = request.paramAsInt(SOURCE_FILE_LINE_NUMBER);

    checkArguments(testUuid, testFileUuid, sourceFileUuid, sourceFileLineNumber);
    checkPermissions(testUuid, testFileUuid, sourceFileUuid);
    List<TestDoc> tests = searchTests(testUuid, testFileUuid, sourceFileUuid, sourceFileLineNumber);
    Map<String, ComponentDto> componentsByTestFileUuid = buildComponentsByTestFileUuid(tests);

    JsonWriter json = response.newJsonWriter().beginObject();
    writeTests(tests, componentsByTestFileUuid, json);
    json.endObject().close();
  }

  private void writeTests(List<TestDoc> tests, Map<String, ComponentDto> componentsByTestFileUuid, JsonWriter json) {
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
      json.prop("fileKey", componentsByTestFileUuid.get(fileUuid).key());
      json.prop("fileLongName", componentsByTestFileUuid.get(fileUuid).longName());
      json.endObject();
    }
    json.endArray();
  }

  private Map<String, ComponentDto> buildComponentsByTestFileUuid(List<TestDoc> tests) {
    List<String> fileUuids = Lists.transform(tests, new NonNullInputFunction<TestDoc, String>() {
      @Override
      protected String doApply(TestDoc testDoc) {
        return testDoc.fileUuid();
      }
    });
    DbSession dbSession = dbClient.openSession(false);
    List<ComponentDto> components;
    try {
      components = dbClient.componentDao().getByUuids(dbSession, fileUuids);
    } finally {
      MyBatis.closeQuietly(dbSession);
    }

    return Maps.uniqueIndex(components, new Function<ComponentDto, String>() {
      @Override
      public String apply(@Nullable ComponentDto componentDto) {
        return componentDto.uuid();
      }
    });
  }

  private List<TestDoc> searchTests(@Nullable String testUuid, @Nullable String testFileUuid, @Nullable String sourceFileUuid, @Nullable Integer sourceFileLineNumber) {
    if (testUuid != null) {
      return Arrays.asList(testIndex.searchByTestUuid(testUuid));
    } else if (testFileUuid != null) {
      return testIndex.searchByTestFileUuid(testFileUuid);
    } else {
      return testIndex.searchBySourceFileUuidAndLineNumber(sourceFileUuid, sourceFileLineNumber);
    }
  }

  private void checkPermissions(@Nullable String testUuid, @Nullable String testFileUuid, @Nullable String sourceFileUuid) {
    if (testUuid != null) {
      UserSession.get().checkComponentUuidPermission(UserRole.CODEVIEWER, testIndex.searchByTestUuid(testUuid).fileUuid());
    } else {
      UserSession.get().checkComponentUuidPermission(UserRole.CODEVIEWER, firstNonNull(testFileUuid, sourceFileUuid));
    }
  }

  private void checkArguments(@Nullable String testUuid, @Nullable String testFileUuid, @Nullable String sourceFileUuid, @Nullable Integer sourceFileLineNumber) {
    checkArgument(testUuid != null || testFileUuid != null || sourceFileUuid != null && sourceFileLineNumber != null,
      "At least one of the following combination must be provided: test UUID; test file UUID; source file UUID and its line number.");
  }
}
