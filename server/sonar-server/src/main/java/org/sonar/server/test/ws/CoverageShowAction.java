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
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.test.CoverageService;

import java.util.Map;

public class CoverageShowAction implements RequestHandler {

  private static final String KEY = "key";
  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String TYPE = "type";

  private final CoverageService coverageService;

  public CoverageShowAction(CoverageService coverageService) {
    this.coverageService = coverageService;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get code coverage. Require Browse permission on file's project<br/>" +
        "Each element of the result array is composed of:" +
        "<ol>" +
        "<li>Line number</li>" +
        "<li>Is the line covered?</li>" +
        "<li>Number of tests covering this line</li>" +
        "<li>Number of branches</li>" +
        "<li>Number of branches covered</li>" +
        "</ol>")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "coverage-example-show.json"))
      .setHandler(this);

    action
      .createParam(KEY)
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam(FROM)
      .setDescription("First line to return. Starts at 1")
      .setExampleValue("10")
      .setDefaultValue("1");

    action
      .createParam(TO)
      .setDescription("Last line to return (inclusive)")
      .setExampleValue("20");

    action
      .createParam(TYPE)
      .setDescription("Type of coverage info to return :" +
        "<ul>" +
        "<li>UT : Unit Tests</li>" +
        "<li>IT : Integration Tests</li>" +
        "<li>OVERALL : Unit and Integration Tests</li>" +
        "</ul>")
      .setPossibleValues(CoverageService.TYPE.values())
      .setDefaultValue(CoverageService.TYPE.UT.name());
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam(KEY);
    coverageService.checkPermission(fileKey);

    int from = Math.max(request.mandatoryParamAsInt(FROM), 1);
    int to = (Integer) ObjectUtils.defaultIfNull(request.paramAsInt(TO), Integer.MAX_VALUE);
    CoverageService.TYPE type = CoverageService.TYPE.valueOf(request.mandatoryParam(TYPE));

    JsonWriter json = response.newJsonWriter().beginObject();

    Map<Integer, Integer> hits = coverageService.getHits(fileKey, type);
    if (!hits.isEmpty()) {
      Map<Integer, Integer> testCases = coverageService.getTestCases(fileKey, type);
      Map<Integer, Integer> conditions = coverageService.getConditions(fileKey, type);
      Map<Integer, Integer> coveredConditions = coverageService.getCoveredConditions(fileKey, type);
      writeCoverage(hits, testCases, conditions, coveredConditions, from, to, json);
    }

    json.endObject().close();
  }

  private void writeCoverage(Map<Integer, Integer> hitsByLine,
                             Map<Integer, Integer> testCasesByLines,
                             Map<Integer, Integer> conditionsByLine,
                             Map<Integer, Integer> coveredConditionsByLine,
                             int from, int to, JsonWriter json) {
    json.name("coverage").beginArray();
    for (Map.Entry<Integer, Integer> entry : hitsByLine.entrySet()) {
      Integer line = entry.getKey();
      if (line >= from && line <= to) {
        Integer hits = entry.getValue();
        json.beginArray();
        json.value(line);
        json.value(hits > 0);
        json.value(testCasesByLines.get(line));
        json.value(conditionsByLine.get(line));
        json.value(coveredConditionsByLine.get(line));
        json.endArray();
      }
    }
    json.endArray();
  }
}
