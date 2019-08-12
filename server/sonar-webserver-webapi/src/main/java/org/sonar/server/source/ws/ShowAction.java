/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.source.ws;

import com.google.common.io.Resources;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class ShowAction implements SourcesWsAction {

  private final SourceService sourceService;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ShowAction(SourceService sourceService, DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.sourceService = sourceService;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get source code. Requires See Source Code permission on file's project<br/>" +
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
    int from = Math.max(request.paramAsInt("from"), 1);
    int to = (Integer) ObjectUtils.defaultIfNull(request.paramAsInt("to"), Integer.MAX_VALUE);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto file = componentFinder.getByKey(dbSession, fileKey);
      userSession.checkComponentPermission(UserRole.CODEVIEWER, file);

      Iterable<String> linesHtml = checkFoundWithOptional(sourceService.getLinesAsHtml(dbSession, file.uuid(), from, to), "No source found for file '%s'", fileKey);
      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        writeSource(linesHtml, from, json);
        json.endObject();
      }
    }
  }

  private static void writeSource(Iterable<String> lines, int from, JsonWriter json) {
    json.name("sources").beginArray();
    long index = 0L;
    for (String line : lines) {
      json.beginArray();
      json.value(index + from);
      json.value(line);
      json.endArray();
      index++;
    }
    json.endArray();
  }
}
