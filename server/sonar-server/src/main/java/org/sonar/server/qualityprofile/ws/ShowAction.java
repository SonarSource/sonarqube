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
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonarqube.ws.QualityProfiles.ShowWsResponse;
import org.sonarqube.ws.QualityProfiles.ShowWsResponse.CompareToSonarWay;

import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_COMPARE_TO_SONAR_WAY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;

public class ShowAction implements QProfileWsAction {

  @Override
  public void define(WebService.NewController controller) {
    NewAction show = controller.createAction("show")
      .setSince("6.5")
      .setDescription("Show a quality profile")
      .setResponseExample(getClass().getResource("show-example.json"))
      .setInternal(true)
      .setHandler(this);

    show.createParam(PARAM_PROFILE)
      .setDescription("Quality profile key")
      .setExampleValue(UUID_EXAMPLE_01)
      .setRequired(true);

    show.createParam(PARAM_COMPARE_TO_SONAR_WAY)
      .setDescription("Add the number of missing rules from the related Sonar way profile in the response")
      .setInternal(true)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ShowWsResponse showWsResponse = ShowWsResponse.newBuilder()
      .setProfile(ShowWsResponse.QualityProfile.newBuilder()
        .setKey("AU-TpxcA-iU5OvuD2FL3")
        .setName("My Company Profile")
        .setLanguage("cs")
        .setLanguageName("C#")
        .setIsInherited(true)
        .setIsBuiltIn(false)
        .setIsDefault(false)
        .setParentKey("AU-TpxcA-iU5OvuD2FL1")
        .setParentName("Parent Company Profile")
        .setActiveRuleCount(10)
        .setActiveDeprecatedRuleCount(0)
        .setProjectCount(7)
        .setRuleUpdatedAt("2016-12-22T19:10:03+0100")
        .setLastUsed("2016-12-01T19:10:03+0100"))
      .setCompareToSonarWay(CompareToSonarWay.newBuilder()
        .setProfile("iU5OvuD2FLz")
        .setProfileName("Sonar way")
        .setMissingRuleCount(4)
        .build())
      .build();
    writeProtobuf(showWsResponse, request, response);
  }

}
