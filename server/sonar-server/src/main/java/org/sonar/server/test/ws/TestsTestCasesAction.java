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

import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import org.sonar.api.component.Component;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.Testable;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentService;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class TestsTestCasesAction implements RequestHandler {

  private static final String KEY = "key";
  private static final String UUID = "uuid";
  private static final String LINE = "line";

  private final SnapshotPerspectives snapshotPerspectives;
  private final ComponentService componentService;
  private final DbClient dbClient;

  public TestsTestCasesAction(SnapshotPerspectives snapshotPerspectives, ComponentService componentService, DbClient dbClient) {
    this.snapshotPerspectives = snapshotPerspectives;
    this.componentService = componentService;
    this.dbClient = dbClient;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("test_cases")
      .setDescription("Get the list of test cases covering a given file and line. Require Browse permission on file's project")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-testable.json"))
      .setHandler(this);

    action
      .createParam(KEY)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam(UUID)
      .setDescription("File UUID")
      .setExampleValue("584a89f2-8037-4f7b-b82c-8b45d2d63fb2");

    action
      .createParam(LINE)
      .setRequired(true)
      .setDescription("Line of the file used to get test cases")
      .setExampleValue("10");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.param(KEY);
    String fileUuid = request.param(UUID);
    Preconditions.checkArgument(fileKey != null || fileUuid != null, "At least one of 'key' or 'uuid' must be provided");

    if (fileKey != null) {
      UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
    } else {
      ComponentDto component = componentService.getByUuid(fileUuid);
      fileKey = component.getKey();
      UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
    }
    int line = request.mandatoryParamAsInt(LINE);

    Testable testable = snapshotPerspectives.as(MutableTestable.class, fileKey);
    JsonWriter json = response.newJsonWriter().beginObject();
    if (testable != null) {
      Map<String, Integer> refByTestPlan = newHashMap();
      Map<String, Component> componentsByKey = newHashMap();
      writeTests(testable, line, refByTestPlan, componentsByKey, json);
      writeFiles(refByTestPlan, componentsByKey, json);
    }
    json.endObject().close();
  }

  private void writeTests(Testable testable, Integer line, Map<String, Integer> refByTestPlan, Map<String, Component> componentsByKey, JsonWriter json) {
    json.name("tests").beginArray();
    for (TestCase testCase : testable.testCasesOfLine(line)) {
      json.beginObject();
      json.prop("name", testCase.name());
      json.prop("status", testCase.status().name());
      json.prop("durationInMs", testCase.durationInMs());

      Component testPlan = testCase.testPlan().component();
      Integer ref = refByTestPlan.get(testPlan.key());
      if (ref == null) {
        ref = refByTestPlan.size() + 1;
        refByTestPlan.put(testPlan.key(), ref);
        componentsByKey.put(testPlan.key(), testPlan);
      }
      json.prop("_ref", Integer.toString(ref));
      json.endObject();
    }
    json.endArray();
  }

  private void writeFiles(Map<String, Integer> refByTestPlan, Map<String, Component> componentsByKey, JsonWriter json) {
    DbSession session = dbClient.openSession(false);
    try {
      for (ComponentDto componentDto : dbClient.componentDao().getByKeys(session, componentsByKey.keySet())) {
        componentsByKey.put(componentDto.key(), componentDto);
      }
    } finally {
      session.close();
    }

    json.name("files").beginObject();
    for (Map.Entry<String, Integer> entry : refByTestPlan.entrySet()) {
      String componentKey = entry.getKey();
      Integer ref = entry.getValue();
      Component file = componentsByKey.get(componentKey);
      json.name(Integer.toString(ref)).beginObject();
      json.prop("key", file.key());
      json.prop("uuid", ((ComponentDto) file).uuid());
      json.prop("longName", file.longName());
      json.endObject();
    }
    json.endObject();
  }
}
