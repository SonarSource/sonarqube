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
import org.sonar.server.qualitygate.QualityGates;
import org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters;

public class SetAsDefaultAction implements QualityGatesWsAction {

  private final QualityGates qualityGates;

  public SetAsDefaultAction(QualityGates qualityGates) {
    this.qualityGates = qualityGates;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("set_as_default")
      .setDescription("Set a quality gate as the default quality gate. Require Administer Quality Gates permission")
      .setSince("4.3")
      .setPost(true)
      .setHandler(this);

    action.createParam(QualityGatesWsParameters.PARAM_ID)
      .setDescription("ID of the quality gate to set as default")
      .setRequired(true)
      .setExampleValue("1");
  }

  @Override
  public void handle(Request request, Response response) {
    qualityGates.setDefault(QualityGatesWs.parseId(request, QualityGatesWsParameters.PARAM_ID));
    response.noContent();
  }

}
