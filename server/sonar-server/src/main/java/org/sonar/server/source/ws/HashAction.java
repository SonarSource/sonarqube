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
import org.apache.commons.io.Charsets;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.source.db.FileSourceDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;

import java.io.OutputStream;

public class HashAction implements RequestHandler {

  private final DbClient dbClient;
  private final FileSourceDao fileSourceDao;

  public HashAction(DbClient dbClient, FileSourceDao fileSourceDao) {
    this.dbClient = dbClient;
    this.fileSourceDao = fileSourceDao;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("hash")
      .setDescription("Show line line hashes for a given file. Require Browse permission on file's project<br/>")
      .setSince("5.0")
      .setInternal(true)
      .setResponseExample(Resources.getResource(getClass(), "example-hash.txt"))
      .setHandler(this);

    action
      .createParam("key")
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("org.codehaus.sonar:sonar-server:src/main/java/org/sonar/server/source/SourceService.java");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession session = dbClient.openSession(false);
    try {
      String componentKey = request.mandatoryParam("key");
      ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);
      if (component == null) {
        throw new NotFoundException("Unable to find component with key " + componentKey);
      }
      String lineHashes = fileSourceDao.selectLineHashes(component.uuid(), session);
      if (lineHashes == null) {
        response.noContent();
      } else {
        OutputStream output = response.stream().setMediaType("text/plain").output();
        output.write(lineHashes.getBytes(Charsets.UTF_8));
        output.close();
      }
    } finally {
      session.close();
    }
  }
}
