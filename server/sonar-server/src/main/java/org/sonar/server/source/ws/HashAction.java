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

import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_001;

public class HashAction implements SourcesWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public HashAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
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
      .setExampleValue(KEY_FILE_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession session = dbClient.openSession(false)) {
      String componentKey = request.mandatoryParam("key");
      ComponentDto component = componentFinder.getByKey(session, componentKey);
      userSession.checkComponentPermission(UserRole.USER, component);

      response.stream().setMediaType("text/plain");
      try (OutputStreamWriter writer = new OutputStreamWriter(response.stream().output(), StandardCharsets.UTF_8)) {
        HashFunction hashFunction = new HashFunction(writer, componentKey);
        dbClient.fileSourceDao().readLineHashesStream(session, component.uuid(), hashFunction);
        if (!hashFunction.hasData()) {
          response.noContent();
        }
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
