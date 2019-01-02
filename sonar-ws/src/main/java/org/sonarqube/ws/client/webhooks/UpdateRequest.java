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

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/webhooks/update">Further information about this action online (including a response example)</a>
 * @since 7.1
 */
@Generated("sonar-ws-generator")
public class UpdateRequest {

  private String name;
  private String url;
  private String webhook;

  /**
   * This is a mandatory parameter.
   * Example value: "My Webhook"
   */
  public UpdateRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "https://www.my-webhook-listener.com/sonar"
   */
  public UpdateRequest setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getUrl() {
    return url;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "my_project"
   */
  public UpdateRequest setWebhook(String webhook) {
    this.webhook = webhook;
    return this;
  }

  public String getWebhook() {
    return webhook;
  }
}
