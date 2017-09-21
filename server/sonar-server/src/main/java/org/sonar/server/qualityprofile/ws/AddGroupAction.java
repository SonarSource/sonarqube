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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.qualityprofile.ws.QProfileWsSupport.createOrganizationParam;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;

public class AddGroupAction implements QProfileWsAction {

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_ADD_GROUP)
      .setDescription("Allow a group to edit a Quality Profile.<br>" +
        "Requires the 'Administer Quality Profiles' permission or the ability to edit the quality profile.")
      .setHandler(this)
      .setPost(true)
      .setInternal(true)
      .setSince("6.6");

    action.createParam(PARAM_PROFILE)
      .setDescription("Quality Profile key.")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_GROUP)
      .setDescription("Group name")
      .setRequired(true)
      .setExampleValue("sonar-administrators");

    createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // TODO
  }
}
