/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client.editions;

import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class EditionsService extends BaseService {

  public EditionsService(WsConnector wsConnector) {
    super(wsConnector, "api/editions");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/is_valid_license">Further information about this action online (including a response example)</a>
   * @since 7.3
   */
  public String isValidLicense() {
    return call(
      new GetRequest(path("is_valid_license"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/set_license">Further information about this action online (including a response example)</a>
   * @since 7.2
   */
  public void setLicense(SetLicenseRequest request) {
    call(
      new PostRequest(path("set_license"))
        .setParam("license", request.getLicense())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/show_license">Further information about this action online (including a response example)</a>
   * @since 7.2
   */
  public String showLicense() {
    return call(
      new GetRequest(path("show_license"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/unset_license">Further information about this action online (including a response example)</a>
   * @since 7.2
   */
  public void unsetLicense() {
    call(
      new PostRequest(path("unset_license"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
