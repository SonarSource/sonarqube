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
package org.sonarqube.ws.client.qualitygates;

import java.util.List;
import javax.annotation.Generated;

/**
 * Search for projects associated (or not) to a quality gate.<br/>Only authorized projects for current user will be returned.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/search">Further information about this action online (including a response example)</a>
 * @since 4.3
 */
@Generated("sonar-ws-generator")
public class SearchRequest {

  private String gateId;
  private String page;
  private String pageSize;
  private String query;
  private String selected;

  /**
   * Quality Gate ID
   *
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
   * Page number
   *
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
   * Page size
   *
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
   * To search for projects containing this string. If this parameter is set, "selected" is set to "all".
   *
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
   * Depending on the value, show only selected items (selected=selected), deselected items (selected=deselected), or all items with their selection status (selected=all).
   *
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
