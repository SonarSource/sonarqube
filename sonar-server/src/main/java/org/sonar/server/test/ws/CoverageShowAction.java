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
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.test.CoverageService;

import java.util.Map;

public class CoverageShowAction implements RequestHandler {

  private final CoverageService coverageService;

  public CoverageShowAction(CoverageService coverageService) {
    this.coverageService = coverageService;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get code coverage. Require Browse permission on file's project")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "coverage-example-show.json"))
      .setHandler(this);

    action
      .createParam("key")
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam("from")
      .setDescription("First line to return. Starts at 1")
      .setExampleValue("10")
      .setDefaultValue("1");

    action
      .createParam("to")
      .setDescription("Last line to return (inclusive)")
      .setExampleValue("20");

    action
      .createParam("type")
      .setDescription("Type of coverage info to return :" +
        "<ul>" +
        "<li>UT : Unit Tests</li>" +
        "<li>IT : Integration Tests</li>" +
        "<li>OVERALL : Unit and Integration Tests</li>" +
        "</ul>")
      .setPossibleValues("UT", "IT", "OVERALL");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam("key");
    coverageService.checkPermission(fileKey);

    int from = Math.max(request.mandatoryParamAsInt("from"), 1);
    int to = (Integer) ObjectUtils.defaultIfNull(request.paramAsInt("to"), Integer.MAX_VALUE);

    JsonWriter json = response.newJsonWriter().beginObject();

    String hits = coverageService.getHitsData(fileKey);
    if (hits != null) {
      Map<Integer, Integer> hitsByLine =  KeyValueFormat.parseIntInt(hits);
    writeCoverage(hitsByLine, from, to, json);
    }

    json.endObject().close();
  }

  private void writeCoverage(Map<Integer, Integer> hitsByLine, int from, int to, JsonWriter json) {
    json.name("coverage").beginArray();
    for (Map.Entry<Integer, Integer> entry : hitsByLine.entrySet()) {
      Integer line = entry.getKey();
      if (line >= from && line <= to) {
        Integer hits = entry.getValue();
        json.beginArray();
        json.value(line);
        json.value(hits);
        json.endArray();
      }
    }
    json.endArray();
  }
}
