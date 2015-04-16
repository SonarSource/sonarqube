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

package org.sonar.server.qualitygate.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.qualitygate.QualityGates;

public class QGatesCopyAction implements BaseQGateWsAction {

  private final QualityGates qualityGates;

  public QGatesCopyAction(QualityGates qualityGates) {
    this.qualityGates = qualityGates;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("copy")
      .setDescription("Copy a Quality Gate. Require Administer Quality Profiles and Gates permission")
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    action.createParam(QGatesWs.PARAM_ID)
      .setDescription("The ID of the source quality gate")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(QGatesWs.PARAM_NAME)
      .setDescription("The name of the quality gate to create")
      .setRequired(true)
      .setExampleValue("My Quality Gate");
  }

  @Override
  public void handle(Request request, Response response) {
    QualityGateDto newQualityGate = qualityGates.copy(QGatesWs.parseId(request, QGatesWs.PARAM_ID), request.mandatoryParam(QGatesWs.PARAM_NAME));
    JsonWriter writer = response.newJsonWriter();
    QGatesWs.writeQualityGate(newQualityGate, writer).close();
  }

}
