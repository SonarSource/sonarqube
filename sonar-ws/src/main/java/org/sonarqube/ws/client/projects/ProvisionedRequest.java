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
package org.sonarqube.ws.client.projects;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/provisioned">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class ProvisionedRequest {

  private List<String> f;
  private String organization;
  private String p;
  private String ps;
  private String q;

  /**
   * Possible values:
   * <ul>
   *   <li>"name"</li>
   *   <li>"creationDate"</li>
   *   <li>"visibility"</li>
   *   <li>"uuid"</li>
   *   <li>"key"</li>
   * </ul>
   */
  public ProvisionedRequest setF(List<String> f) {
    this.f = f;
    return this;
  }

  public List<String> getF() {
    return f;
  }

  /**
   * This is part of the internal API.
   */
  public ProvisionedRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "42"
   */
  public ProvisionedRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public ProvisionedRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Example value: "sonar"
   */
  public ProvisionedRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }
}
