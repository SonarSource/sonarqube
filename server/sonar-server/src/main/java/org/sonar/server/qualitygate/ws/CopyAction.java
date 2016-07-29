/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.qualitygate.QualityGates;

import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_NAME;

public class CopyAction implements QualityGatesWsAction {

  private final QualityGates qualityGates;

  public CopyAction(QualityGates qualityGates) {
    this.qualityGates = qualityGates;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("copy")
      .setDescription("Copy a Quality Gate. Require Administer Quality Gates permission")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("The ID of the source quality gate")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(PARAM_NAME)
      .setDescription("The name of the quality gate to create")
      .setRequired(true)
      .setExampleValue("My Quality Gate");
  }

  @Override
  public void handle(Request request, Response response) {
    QualityGateDto newQualityGate = qualityGates.copy(QualityGatesWs.parseId(request, PARAM_ID), request.mandatoryParam(PARAM_NAME));
    JsonWriter writer = response.newJsonWriter();
    QualityGatesWs.writeQualityGate(newQualityGate, writer).close();
  }

}
