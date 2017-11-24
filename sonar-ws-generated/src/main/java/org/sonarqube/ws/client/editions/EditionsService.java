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
package org.sonarqube.ws.client.editions;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Editions.FormDataResponse;
import org.sonarqube.ws.Editions.PreviewResponse;
import org.sonarqube.ws.Editions.StatusResponse;

/**
 * Manage SonarSource commercial editions.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class EditionsService extends BaseService {

  public EditionsService(WsConnector wsConnector) {
    super(wsConnector, "api/editions");
  }

  /**
   * Apply changes to SonarQube to match the specified license. Clear error message of previous automatic install of an edition, if there is any. Require 'Administer System' permission.
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/apply_license">Further information about this action online (including a response example)</a>
   * @since 6.7
   */
  public String applyLicense(ApplyLicenseRequest request) {
    return call(
      new PostRequest(path("apply_license"))
        .setParam("license", request.getLicense())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Clear error message of last install of an edition (if any). Require 'Administer System' permission.
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/clear_error_message">Further information about this action online (including a response example)</a>
   * @since 6.7
   */
  public void clearErrorMessage() {
    call(
      new PostRequest(path("clear_error_message"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   * Provide data to prefill license request forms: the server ID and the total number of lines of code.
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/form_data">Further information about this action online (including a response example)</a>
   * @since 6.7
   */
  public FormDataResponse formData() {
    return call(
      new GetRequest(path("form_data")),
      FormDataResponse.parser());
  }

  /**
   * Preview the changes to SonarQube to match the specified license. Requires 'Administer System' permission.
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/preview">Further information about this action online (including a response example)</a>
   * @since 6.7
   */
  public PreviewResponse preview(PreviewRequest request) {
    return call(
      new PostRequest(path("preview"))
        .setParam("license", request.getLicense()),
      PreviewResponse.parser());
  }

  /**
   * Provide status of SonarSource commercial edition of the current SonarQube. Requires 'Administer System' permission.
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/status">Further information about this action online (including a response example)</a>
   * @since 6.7
   */
  public StatusResponse status() {
    return call(
      new GetRequest(path("status")),
      StatusResponse.parser());
  }

  /**
   * Uninstall the currently installed edition. Requires 'Administer System' permission.
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/editions/uninstall">Further information about this action online (including a response example)</a>
   * @since 6.7
   */
  public void uninstall() {
    call(
      new PostRequest(path("uninstall"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
