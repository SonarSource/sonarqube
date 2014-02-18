/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.qualitygate.QualityGates;

public class QualityGatesWs implements WebService {

  private final QualityGates qualityGates;

  public QualityGatesWs(QualityGates qualityGates) {
    this.qualityGates = qualityGates;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.newController("api/qualitygates")
      .setSince("4.3")
      .setDescription("This service can be used to manage quality gates, including requirements and project association.");
    controller.newAction("create")
      .setDescription("Create a quality gate, given its name.")
      .setPost(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          create(request, response);
        }
    }).newParam("name");
    controller.newAction("list")
    .setDescription("List all quality gates.")
    .setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        list(request, response);
      }
    });
    controller.done();
  }

  protected void list(Request request, Response response) {
    JsonWriter writer = response.newJsonWriter().beginObject().name("qualitygates").beginArray();
    for (QualityGateDto qgate: qualityGates.list()) {
      writeQualityGate(qgate, writer);
    }
    writer.endArray().endObject().close();
  }

  protected void create(Request request, Response response) {
    QualityGateDto newQualityGate = qualityGates.create(request.requiredParam("name"));
    JsonWriter writer = response.newJsonWriter();
    writeQualityGate(newQualityGate, writer).close();
  }

  private JsonWriter writeQualityGate(QualityGateDto newQualityGate, JsonWriter writer) {
    return writer.beginObject()
      .prop("id", newQualityGate.getId())
      .prop("name", newQualityGate.getName())
      .endObject();
  }

}
