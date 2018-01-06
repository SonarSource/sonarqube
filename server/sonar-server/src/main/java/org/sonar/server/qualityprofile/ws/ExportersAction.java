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

import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;

public class ExportersAction implements QProfileWsAction {

  private ProfileExporter[] exporters;

  public ExportersAction(ProfileExporter[] exporters) {
    this.exporters = exporters;
  }

  /**
   * Used by Pico if no {@link ProfileExporter} is found
   */
  public ExportersAction() {
    this(new ProfileExporter[0]);
  }

  @Override
  public void define(NewController context) {
    context.createAction("exporters")
      .setDescription("Lists available profile export formats.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("exporters-example.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("exporters").beginArray();
      for (ProfileExporter exporter : exporters) {
        json.beginObject()
          .prop("key", exporter.getKey())
          .prop("name", exporter.getName());
        json.name("languages").beginArray();
        for (String language : exporter.getSupportedLanguages()) {
          json.value(language);
        }
        json.endArray().endObject();
      }
      json.endArray().endObject();
    }
  }

}
