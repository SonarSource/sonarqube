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
package org.sonarqube.ws.client.qualityprofile;

import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

public class QualityProfilesService extends BaseService {

  public QualityProfilesService(WsConnector wsConnector) {
    super(wsConnector, "api/qualityprofiles");
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("defaults", request.getDefaults())
        .setParam("language", request.getLanguage())
        .setParam("profileName", request.getProfileName())
        .setParam("projectKey", request.getProjectKey()),
      SearchWsResponse.parser());
  }

}
