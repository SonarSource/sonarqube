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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

import java.util.List;

public class IndexAction implements RequestHandler {

  private final DbClient dbClient;
  private final SourceService sourceService;

  public IndexAction(DbClient dbClient, SourceService sourceService) {
    this.dbClient = dbClient;
    this.sourceService = sourceService;
  }

  void define(WebService.NewController controller) {
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
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
    Integer from = request.mandatoryParamAsInt("from");
    Integer to = request.paramAsInt("to");
    try (DbSession session = dbClient.openSession(false)) {
      ComponentDto componentDto = dbClient.componentDao().getByKey(session, fileKey);
      List<String> lines = sourceService.getLinesAsTxt(componentDto.uuid(), from, to == null ? null : to - 1);
      JsonWriter json = response.newJsonWriter().beginArray().beginObject();
      Integer lineCounter = from;
      for (String line : lines) {
        json.prop(lineCounter.toString(), line);
        lineCounter++;
      }
      json.endObject().endArray().close();
    }
  }
}
