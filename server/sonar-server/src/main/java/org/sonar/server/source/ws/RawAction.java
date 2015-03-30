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
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

import java.io.IOException;
import java.util.List;

public class RawAction implements SourcesAction {

  private final DbClient dbClient;
  private final SourceService sourceService;

  public RawAction(DbClient dbClient, SourceService sourceService) {
    this.dbClient = dbClient;
    this.sourceService = sourceService;
  }

  @Override
  public void  define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("raw")
      .setDescription("Get source code as plain text. Require See Source Code permission on file")
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
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
    try (DbSession session = dbClient.openSession(false)) {
      ComponentDto componentDto = dbClient.componentDao().getByKey(session, fileKey);
      List<String> lines = sourceService.getLinesAsTxt(componentDto.uuid(), null, null);
      response.stream().setMediaType("text/plain");
      IOUtils.writeLines(lines, "\n", response.stream().output(), Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to write raw source of file " + fileKey, e);
    }
  }
}
