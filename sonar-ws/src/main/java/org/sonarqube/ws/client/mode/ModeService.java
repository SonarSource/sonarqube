/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarqube.ws.client.mode;

import com.google.gson.Gson;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PatchRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;

public class ModeService extends BaseService {

  public ModeService(WsConnector wsConnector) {
    super(wsConnector, "api/v2");
  }

  public void setMode(ModeResource.ModeEnum mode) {
    callEndpointToPatchModeMapping(new ModeResource(mode, null)).close();
  }

  private WsResponse callEndpointToPatchModeMapping(ModeResource request) {
    return call(
      new PatchRequest(path("clean-code-policy/mode"))
        .setBody(new Gson().toJson(request))
        .setMediaType(MediaTypes.JSON));
  }

  public ModeResource getMode() {
    return new Gson().fromJson(call(
      new GetRequest(path("clean-code-policy/mode"))
        .setMediaType(MediaTypes.JSON)).content(),
      ModeResource.class);
  }

}
