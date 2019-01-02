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
package org.sonarqube.ws.client.issues;

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/component_tags">Further information about this action online (including a response example)</a>
 * @since 5.1
 */
@Generated("sonar-ws-generator")
public class ComponentTagsRequest {

  private String componentUuid;
  private String createdAfter;
  private String ps;

  /**
   * This is a mandatory parameter.
   * Example value: "7d8749e8-3070-4903-9188-bdd82933bb92"
   */
  public ComponentTagsRequest setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  /**
   * Example value: "2017-10-19 or 2017-10-19T13:00:00+0200"
   */
  public ComponentTagsRequest setCreatedAfter(String createdAfter) {
    this.createdAfter = createdAfter;
    return this;
  }

  public String getCreatedAfter() {
    return createdAfter;
  }

  /**
   * Example value: "25"
   */
  public ComponentTagsRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }
}
