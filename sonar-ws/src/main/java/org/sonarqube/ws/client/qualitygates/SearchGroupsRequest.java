/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import jakarta.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/search_groups">Further information about this action online (including a response example)</a>
 * @since 9.2
 */
@Generated("sonar-ws-generator")
public class SearchGroupsRequest {

  private String p;
  private String ps;
  private String q;
  private String qualityGate;
  private String selected;

  /**
   * Example value: "42"
   */
  public SearchGroupsRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public SearchGroupsRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Example value: "sonar"
   */
  public SearchGroupsRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "SonarSource Way"
   */
  public SearchGroupsRequest setQualityGate(String qualityGate) {
    this.qualityGate = qualityGate;
    return this;
  }

  public String getQualityGate() {
    return qualityGate;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"all"</li>
   *   <li>"deselected"</li>
   *   <li>"selected"</li>
   * </ul>
   */
  public SearchGroupsRequest setSelected(String selected) {
    this.selected = selected;
    return this;
  }

  public String getSelected() {
    return selected;
  }
}
