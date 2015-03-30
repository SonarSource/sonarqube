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

import com.google.common.base.Function;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;

public class HashAction implements SourcesAction {

  private final DbClient dbClient;

  public HashAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("hash")
      .setDescription("Show line line hashes for a given file. Require See Source Code permission on file's project<br/>")
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
    try (DbSession session = dbClient.openSession(false)) {
      final String componentKey = request.mandatoryParam("key");
      final ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);
      UserSession.get().checkProjectUuidPermission(UserRole.USER, component.projectUuid());

      response.stream().setMediaType("text/plain");
      OutputStreamWriter writer = new OutputStreamWriter(response.stream().output(), Charsets.UTF_8);
      try {
        HashFunction hashFunction = new HashFunction(writer, componentKey);
        dbClient.fileSourceDao().readLineHashesStream(session, component.uuid(), hashFunction);
        if (!hashFunction.hasData()) {
          response.noContent();
        }
      } finally {
        writer.close();
      }
    }
  }

  private static class HashFunction implements Function<Reader, Void> {

    private final OutputStreamWriter writer;
    private final String componentKey;
    private boolean hasData = false;

    public HashFunction(OutputStreamWriter writer, String componentKey) {
      this.writer = writer;
      this.componentKey = componentKey;
    }

    @Override
    public Void apply(Reader input) {
      try {
        hasData = true;
        CharStreams.copy(input, writer);
      } catch (IOException e) {
        throw new IllegalStateException(String.format("Can't read line hashes of file '%s'", componentKey), e);
      }
      return null;
    }

    public boolean hasData() {
      return hasData;
    }
  }

}
