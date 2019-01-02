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
package org.sonarqube.ws.client.governancereports;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class GovernanceReportsService extends BaseService {

  public GovernanceReportsService(WsConnector wsConnector) {
    super(wsConnector, "api/governance_reports");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports/download">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public String download(DownloadRequest request) {
    return call(
      new GetRequest(path("download"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentKey", request.getComponentKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports/status">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public String status(StatusRequest request) {
    return call(
      new GetRequest(path("status"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentKey", request.getComponentKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports/subscribe">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void subscribe(SubscribeRequest request) {
    call(
      new PostRequest(path("subscribe"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentKey", request.getComponentKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports/unsubscribe">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void unsubscribe(UnsubscribeRequest request) {
    call(
      new PostRequest(path("unsubscribe"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentKey", request.getComponentKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports/update_frequency">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void updateFrequency(UpdateFrequencyRequest request) {
    call(
      new PostRequest(path("update_frequency"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentKey", request.getComponentKey())
        .setParam("frequency", request.getFrequency())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports/update_recipients">Further information about this action online (including a response example)</a>
   * @since 1.0
   */
  public void updateRecipients(UpdateRecipientsRequest request) {
    call(
      new PostRequest(path("update_recipients"))
        .setParam("componentId", request.getComponentId())
        .setParam("componentKey", request.getComponentKey())
        .setParam("recipients", request.getRecipients() == null ? null : request.getRecipients().stream().collect(Collectors.joining(",")))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
