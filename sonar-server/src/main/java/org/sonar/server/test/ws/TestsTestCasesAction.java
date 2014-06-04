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
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.server.user.UserSession;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class TestsTestCasesAction implements RequestHandler {

  private static final String KEY = "key";
  private static final String LINE = "line";

  private final SnapshotPerspectives snapshotPerspectives;

  public TestsTestCasesAction(SnapshotPerspectives snapshotPerspectives) {
    this.snapshotPerspectives = snapshotPerspectives;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("test_cases")
      .setDescription("Get the list of test cases covering a given file and line. Require Browse permission on file's project")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-testable.json"))
      .setHandler(this);

    action
      .createParam(KEY)
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam(LINE)
      .setRequired(true)
      .setDescription("Line of the file used to get test cases")
      .setExampleValue("10");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam(KEY);
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
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
    json.name("files").beginObject();
    for (Map.Entry<String, Integer> entry : refByTestPlan.entrySet()) {
      String componentKey = entry.getKey();
      Integer ref = entry.getValue();
      Component file = componentsByKey.get(componentKey);
      json.name(Integer.toString(ref)).beginObject();
      json.prop("key", file.key());
      json.prop("longName", file.longName());
      json.endObject();
    }
    json.endObject();
  }
}
