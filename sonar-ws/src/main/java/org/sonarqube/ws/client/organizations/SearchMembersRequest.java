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
package org.sonarqube.ws.client.organizations;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/search_members">Further information about this action online (including a response example)</a>
 * @since 6.4
 */
@Generated("sonar-ws-generator")
public class SearchMembersRequest {

  private String organization;
  private String p;
  private String ps;
  private String q;
  private String selected;

  /**
   * This is part of the internal API.
   */
  public SearchMembersRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "42"
   */
  public SearchMembersRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public SearchMembersRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Example value: "orwe"
   */
  public SearchMembersRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }

  /**
   * This is part of the internal API.
   * Possible values:
   * <ul>
   *   <li>"selected"</li>
   *   <li>"deselected"</li>
   * </ul>
   */
  public SearchMembersRequest setSelected(String selected) {
    this.selected = selected;
    return this;
  }

  public String getSelected() {
    return selected;
  }
}
