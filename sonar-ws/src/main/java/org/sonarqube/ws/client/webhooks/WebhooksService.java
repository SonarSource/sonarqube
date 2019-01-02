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
package org.sonarqube.ws.client.webhooks;

import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Webhooks.CreateWsResponse;
import org.sonarqube.ws.Webhooks.DeliveriesWsResponse;
import org.sonarqube.ws.Webhooks.DeliveryWsResponse;
import org.sonarqube.ws.Webhooks.ListResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class WebhooksService extends BaseService {

  public WebhooksService(WsConnector wsConnector) {
    super(wsConnector, "api/webhooks");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks/create">Further information about this action online (including a response example)</a>
   * @since 7.1
   */
  public CreateWsResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setParam("project", request.getProject())
        .setParam("url", request.getUrl()),
      CreateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks/delete">Further information about this action online (including a response example)</a>
   * @since 7.1
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("webhook", request.getWebhook())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks/deliveries">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public DeliveriesWsResponse deliveries(DeliveriesRequest request) {
    return call(
      new GetRequest(path("deliveries"))
        .setParam("ceTaskId", request.getCeTaskId())
        .setParam("componentKey", request.getComponentKey())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("webhook", request.getWebhook()),
      DeliveriesWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks/delivery">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public DeliveryWsResponse delivery(DeliveryRequest request) {
    return call(
      new GetRequest(path("delivery"))
        .setParam("deliveryId", request.getDeliveryId()),
      DeliveryWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks/list">Further information about this action online (including a response example)</a>
   * @since 7.1
   */
  public ListResponse list(ListRequest request) {
    return call(
      new GetRequest(path("list"))
        .setParam("organization", request.getOrganization())
        .setParam("project", request.getProject()),
      ListResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks/update">Further information about this action online (including a response example)</a>
   * @since 7.1
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("name", request.getName())
        .setParam("url", request.getUrl())
        .setParam("webhook", request.getWebhook())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
