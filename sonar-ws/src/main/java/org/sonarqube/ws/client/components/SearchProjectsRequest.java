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
package org.sonarqube.ws.client.components;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/search_projects">Further information about this action online (including a response example)</a>
 * @since 6.2
 */
@Generated("sonar-ws-generator")
public class SearchProjectsRequest {

  private String asc;
  private List<String> f;
  private List<String> facets;
  private String filter;
  private String organization;
  private String p;
  private String ps;
  private String s;

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchProjectsRequest setAsc(String asc) {
    this.asc = asc;
    return this;
  }

  public String getAsc() {
    return asc;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"analysisDate"</li>
   *   <li>"leakPeriodDate"</li>
   * </ul>
   */
  public SearchProjectsRequest setF(List<String> f) {
    this.f = f;
    return this;
  }

  public List<String> getF() {
    return f;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"alert_status"</li>
   *   <li>"coverage"</li>
   *   <li>"duplicated_lines_density"</li>
   *   <li>"languages"</li>
   *   <li>"ncloc"</li>
   *   <li>"new_coverage"</li>
   *   <li>"new_duplicated_lines_density"</li>
   *   <li>"new_lines"</li>
   *   <li>"new_maintainability_rating"</li>
   *   <li>"new_reliability_rating"</li>
   *   <li>"new_security_rating"</li>
   *   <li>"reliability_rating"</li>
   *   <li>"security_rating"</li>
   *   <li>"sqale_rating"</li>
   *   <li>"tags"</li>
   * </ul>
   */
  public SearchProjectsRequest setFacets(List<String> facets) {
    this.facets = facets;
    return this;
  }

  public List<String> getFacets() {
    return facets;
  }

  /**
   */
  public SearchProjectsRequest setFilter(String filter) {
    this.filter = filter;
    return this;
  }

  public String getFilter() {
    return filter;
  }

  /**
   * This is part of the internal API.
   */
  public SearchProjectsRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "42"
   */
  public SearchProjectsRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public SearchProjectsRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"alert_status"</li>
   *   <li>"analysisDate"</li>
   *   <li>"coverage"</li>
   *   <li>"duplicated_lines_density"</li>
   *   <li>"lines"</li>
   *   <li>"name"</li>
   *   <li>"ncloc"</li>
   *   <li>"ncloc_language_distribution"</li>
   *   <li>"new_coverage"</li>
   *   <li>"new_duplicated_lines_density"</li>
   *   <li>"new_lines"</li>
   *   <li>"new_maintainability_rating"</li>
   *   <li>"new_reliability_rating"</li>
   *   <li>"new_security_rating"</li>
   *   <li>"reliability_rating"</li>
   *   <li>"security_rating"</li>
   *   <li>"sqale_rating"</li>
   * </ul>
   */
  public SearchProjectsRequest setS(String s) {
    this.s = s;
    return this;
  }

  public String getS() {
    return s;
  }
}
