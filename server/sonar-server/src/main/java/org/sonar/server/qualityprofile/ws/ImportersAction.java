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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

public class ImportersAction implements QProfileWsAction {

  private final ProfileImporter[] importers;

  public ImportersAction(ProfileImporter[] importers) {
    this.importers = importers;
  }

  public ImportersAction() {
    this(new ProfileImporter[0]);
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("importers")
      .setSince("5.2")
      .setDescription("List supported importers.")
      .setResponseExample(getClass().getResource("importers-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("importers").beginArray();
      for (ProfileImporter importer : importers) {
        json.beginObject()
          .prop("key", importer.getKey())
          .prop("name", importer.getName())
          .name("languages").beginArray();
        for (String languageKey : importer.getSupportedLanguages()) {
          json.value(languageKey);
        }
        json.endArray().endObject();
      }
      json.endArray().endObject();
    }
  }
}
