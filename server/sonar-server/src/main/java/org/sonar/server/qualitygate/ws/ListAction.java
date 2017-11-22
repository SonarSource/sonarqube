/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGates;

import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_NAME;

public class ListAction implements QualityGatesWsAction {

  private final QualityGates qualityGates;

  public ListAction(QualityGates qualityGates) {
    this.qualityGates = qualityGates;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("list")
      .setDescription("Get a list of quality gates")
      .setSince("4.3")
      .setResponseExample(Resources.getResource(this.getClass(), "example-list.json"))
      .setChangelog(
        new Change("7.0", "'isDefault' field is added on quality gate level"),
        new Change("7.0", "'default' field on root level is deprecated"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    try (JsonWriter writer = response.newJsonWriter()) {
      QualityGateDto defaultQgate = qualityGates.getDefault();
      Long defaultQgateId = defaultQgate == null ? null : defaultQgate.getId();
      writer.beginObject().name("qualitygates").beginArray();
      for (QualityGateDto qualityGate : qualityGates.list()) {
        writer.beginObject()
          .prop(PARAM_ID, qualityGate.getId())
          .prop(PARAM_NAME, qualityGate.getName())
          .prop("isDefault", qualityGate.getId().equals(defaultQgateId))
          .endObject();
      }
      writer.endArray();
      if (defaultQgateId != null) {
        writer.prop("default", defaultQgateId);
      }
      writer.endObject().close();
    }
  }

}
