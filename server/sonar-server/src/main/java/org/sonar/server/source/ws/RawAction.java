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
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;

import java.util.List;

public class RawAction implements RequestHandler {

  private final DbClient dbClient;
  private final SourceService sourceService;

  public RawAction(DbClient dbClient, SourceService sourceService) {
    this.dbClient = dbClient;
    this.sourceService = sourceService;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("raw")
      .setDescription("Get source code as plain text. Require Code viewer permission on file")
      .setSince("5.0")
      .setResponseExample(Resources.getResource(getClass(), "example-raw.txt"))
      .setHandler(this);

    action
      .createParam("key")
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam("key");
    DbSession session = dbClient.openSession(false);
    try {
      dbClient.componentDao().getByKey(session, fileKey);
      List<String> lines = sourceService.getLinesAsTxt(session, fileKey);
      if (lines.isEmpty()) {
        throw new NotFoundException("File '" + fileKey + "' has no sources");
      }
      response.newTxtWriter().values(lines).close();
    } finally {
      session.close();
    }
  }
}
