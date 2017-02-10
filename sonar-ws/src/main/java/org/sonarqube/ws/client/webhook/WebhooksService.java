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
package org.sonarqube.ws.client.webhook;

import org.sonarqube.ws.Webhooks;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @since 6.2
 */
public class WebhooksService extends BaseService {

  public WebhooksService(WsConnector wsConnector) {
    super(wsConnector, "api/webhooks");
  }

  public Webhooks.DeliveryWsResponse delivery(String deliveryId) {
    GetRequest httpRequest = new GetRequest(path("delivery"))
      .setParam("deliveryId", deliveryId);
    return call(httpRequest, Webhooks.DeliveryWsResponse.parser());
  }

  /**
   * @throws org.sonarqube.ws.client.HttpException if HTTP status code is not 2xx.
   */
  public Webhooks.DeliveriesWsResponse deliveries(DeliveriesRequest request) {
    GetRequest httpRequest = new GetRequest(path("deliveries"))
      .setParam("componentKey", request.getComponentKey())
      .setParam("ceTaskId", request.getCeTaskId());
    return call(httpRequest, Webhooks.DeliveriesWsResponse.parser());
  }
}
