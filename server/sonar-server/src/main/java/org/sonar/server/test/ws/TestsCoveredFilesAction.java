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
import org.sonar.api.test.*;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.server.user.UserSession;

public class TestsCoveredFilesAction implements RequestHandler {

  private static final String KEY = "key";
  private static final String TEST = "test";

  private final SnapshotPerspectives snapshotPerspectives;

  public TestsCoveredFilesAction(SnapshotPerspectives snapshotPerspectives) {
    this.snapshotPerspectives = snapshotPerspectives;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("covered_files")
      .setDescription("Get the list of files covered by a test plan. Require Browse permission on file's project")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-plan.json"))
      .setHandler(this);

    action
      .createParam(KEY)
      .setRequired(true)
      .setDescription("Test plan key")
      .setExampleValue("my_project:/src/test/BarTest.java");

    action
      .createParam(TEST)
      .setRequired(true)
      .setDescription("Test case used to list files covered by the test plan")
      .setExampleValue("my_test");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam(KEY);
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
    String test = request.mandatoryParam(TEST);

    MutableTestPlan testPlan = snapshotPerspectives.as(MutableTestPlan.class, fileKey);
    JsonWriter json = response.newJsonWriter().beginObject();
    if (testPlan != null) {
      writeTests(testPlan, test, json);
    }
    json.endObject().close();
  }

  private void writeTests(TestPlan<MutableTestCase> testPlan, String test, JsonWriter json) {
    json.name("files").beginArray();
    for (TestCase testCase : testPlan.testCasesByName(test)) {
      for (CoverageBlock coverageBlock : testCase.coverageBlocks()) {
        json.beginObject();
        Component file = coverageBlock.testable().component();
        json.prop("key", file.key());
        json.prop("longName", file.longName());
        json.prop("coveredLines", coverageBlock.lines().size());
        json.endObject();
      }
    }
    json.endArray();
  }

}
