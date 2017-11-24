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
package org.sonarqube.ws.client.components;

import java.util.List;
import javax.annotation.Generated;

/**
 * Search for projects
 *
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
   * Ascending sort
   *
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
   * Comma-separated list of the fields to be returned in response
   *
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
   * Comma-separated list of the facets to be computed. No facet is computed by default.
   *
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
   * Filter of projects on name, key, measure value, quality gate, language, tag or whether a project is a favorite or not.<br>The filter must be encoded to form a valid URL (for example '=' must be replaced by '%3D').<br>Examples of use:<ul> <li>to filter my favorite projects with a failed quality gate and a coverage greater than or equals to 60% and a coverage strictly lower than 80%:<br>   <code>filter="alert_status = ERROR and isFavorite and coverage >= 60 and coverage < 80"</code></li> <li>to filter projects with a reliability, security and maintainability rating equals or worse than B:<br>   <code>filter="reliability_rating>=2 and security_rating>=2 and sqale_rating>=2"</code></li> <li>to filter projects without duplication data:<br>   <code>filter="duplicated_lines_density = NO_DATA"</code></li></ul>To filter on project name or key, use the 'query' keyword, for instance : <code>filter='query = "Sonar"'</code>.<br><br>To filter on a numeric metric, provide the metric key.<br>These are the supported metric keys:<br><ul><li>alert_status</li><li>coverage</li><li>duplicated_lines_density</li><li>lines</li><li>ncloc</li><li>ncloc_language_distribution</li><li>new_coverage</li><li>new_duplicated_lines_density</li><li>new_lines</li><li>new_maintainability_rating</li><li>new_reliability_rating</li><li>new_security_rating</li><li>reliability_rating</li><li>security_rating</li><li>sqale_rating</li></ul><br>To filter on a rating, provide the corresponding metric key (ex: reliability_rating for reliability rating).<br>The possible values are:<ul> <li>'1' for rating A</li> <li>'2' for rating B</li> <li>'3' for rating C</li> <li>'4' for rating D</li> <li>'5' for rating E</li></ul>To filter on a Quality Gate status use the metric key 'alert_status'. Only the '=' operator can be used.<br>The possible values are:<ul> <li>'OK' for Passed</li> <li>'WARN' for Warning</li> <li>'ERROR' for Failed</li></ul>To filter on language keys use the language key: <ul> <li>to filter on a single language you can use 'language = java'</li> <li>to filter on several languages you must use 'language IN (java, js)'</li></ul>Use the WS api/languages/list to find the key of a language.<br> To filter on tags use the 'tag' keyword:<ul>  <li>to filter on one tag you can use <code>tag = finance</code></li> <li>to filter on several tags you must use <code>tag in (offshore, java)</code></li></ul>
   *
   */
  public SearchProjectsRequest setFilter(String filter) {
    this.filter = filter;
    return this;
  }

  public String getFilter() {
    return filter;
  }

  /**
   * the organization to search projects in
   *
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
   * 1-based page number
   *
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
   * Page size. Must be greater than 0 and less than 500
   *
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
   * Sort projects by numeric metric key, quality gate status (using 'alert_status'), last analysis date (using 'analysisDate'), or by project name.
   *
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
