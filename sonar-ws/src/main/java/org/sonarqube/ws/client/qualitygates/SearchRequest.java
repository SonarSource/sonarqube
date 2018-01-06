/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client.qualitygates;

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/search">Further information about this action online (including a response example)</a>
 * @since 4.3
 */
@Generated("sonar-ws-generator")
public class SearchRequest {

  private String gateId;
  private String organization;
  private String page;
  private String pageSize;
  private String query;
  private String selected;

  /**
   * This is a mandatory parameter.
   * Example value: "1"
   */
  public SearchRequest setGateId(String gateId) {
    this.gateId = gateId;
    return this;
  }

  public String getGateId() {
    return gateId;
  }

  /**
   * Example value: "my-org"
   */
  public SearchRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "2"
   */
  public SearchRequest setPage(String page) {
    this.page = page;
    return this;
  }

  public String getPage() {
    return page;
  }

  /**
   * Example value: "10"
   */
  public SearchRequest setPageSize(String pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public String getPageSize() {
    return pageSize;
  }

  /**
   * Example value: "abc"
   */
  public SearchRequest setQuery(String query) {
    this.query = query;
    return this;
  }

  public String getQuery() {
    return query;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"all"</li>
   *   <li>"deselected"</li>
   *   <li>"selected"</li>
   * </ul>
   */
  public SearchRequest setSelected(String selected) {
    this.selected = selected;
    return this;
  }

  public String getSelected() {
    return selected;
  }
}
