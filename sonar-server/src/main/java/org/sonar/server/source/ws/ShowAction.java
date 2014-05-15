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
package org.sonar.server.source.ws;

import com.google.common.io.Resources;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;

import java.util.List;

public class ShowAction implements RequestHandler {

  private final SourceService sourceService;

  public ShowAction(SourceService sourceService) {
    this.sourceService = sourceService;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get source code. Require Browse permission on file's project<br/>" +
        "Each element of the result array is composed of:" +
        "<ol>" +
        "<li>Line number</li>" +
        "<li>Content of the line</li>" +
        "</ol>")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "example-show.json"))
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
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam("key");
    int from = Math.max(request.mandatoryParamAsInt("from"), 1);
    int to = (Integer) ObjectUtils.defaultIfNull(request.paramAsInt("to"), Integer.MAX_VALUE);

    List<String> sourceHtml = sourceService.getLinesAsHtml(fileKey, from, to);
    if (sourceHtml.isEmpty()) {
      throw new NotFoundException("File '" + fileKey + "' has no sources");
    }

    JsonWriter json = response.newJsonWriter().beginObject();
    writeSource(sourceHtml, from, json);

    json.endObject().close();
  }

  private void writeSource(List<String> lines, int from, JsonWriter json) {
    json.name("sources").beginArray();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      json.beginArray();
      json.value(i + from);
      json.value(line);
      json.endArray();
    }
    json.endArray();
  }
}
