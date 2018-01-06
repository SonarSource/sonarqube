/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Optional;
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

public class IndexAction implements SourcesWsAction {

  private final DbClient dbClient;
  private final SourceService sourceService;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public IndexAction(DbClient dbClient, SourceService sourceService, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.sourceService = sourceService;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("index")
      .setDescription("Get source code as line number / text pairs. Require See Source Code permission on file")
      .setSince("5.0")
      .setResponseExample(Resources.getResource(getClass(), "example-index.json"))
      .setInternal(true)
      .setHandler(this);

    action
      .createParam("resource")
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");

    action
      .createParam("from")
      .setDefaultValue(1)
      .setDescription("First line");

    action
      .createParam("to")
      .setDescription("Last line (excluded). If not specified, all lines are returned until end of file");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam("resource");
    int from = request.mandatoryParamAsInt("from");
    Integer to = request.paramAsInt("to");
    try (DbSession session = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByKey(session, fileKey);
      userSession.checkComponentPermission(UserRole.CODEVIEWER, component);
      Optional<Iterable<String>> lines = sourceService.getLinesAsRawText(session, component.uuid(), from, to == null ? Integer.MAX_VALUE : (to - 1));
      try (JsonWriter json = response.newJsonWriter()) {
        json.beginArray().beginObject();
        if (lines.isPresent()) {
          int lineCounter = from;
          for (String line : lines.get()) {
            json.prop(String.valueOf(lineCounter), line);
            lineCounter++;
          }
        }
        json.endObject().endArray();
      }
    }
  }
}
