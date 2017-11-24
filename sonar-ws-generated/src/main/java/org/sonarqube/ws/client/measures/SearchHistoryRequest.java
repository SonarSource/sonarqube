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
package org.sonarqube.ws.client.measures;

import java.util.List;
import javax.annotation.Generated;

/**
 * Search measures history of a component.<br>Measures are ordered chronologically.<br>Pagination applies to the number of measures for each metric.<br>Requires the following permission: 'Browse' on the specified component
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/search_history">Further information about this action online (including a response example)</a>
 * @since 6.3
 */
@Generated("sonar-ws-generator")
public class SearchHistoryRequest {

  private String branch;
  private String component;
  private String from;
  private List<String> metrics;
  private String p;
  private String ps;
  private String to;

  /**
   * Branch key
   *
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public SearchHistoryRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Component key
   *
   * This is a mandatory parameter.
   * Example value: "my_project"
   */
  public SearchHistoryRequest setComponent(String component) {
    this.component = component;
    return this;
  }

  public String getComponent() {
    return component;
  }

  /**
   * Filter measures created after the given date (inclusive). <br>Either a date (server timezone) or datetime can be provided
   *
   * Example value: "2017-10-19 or 2017-10-19T13:00:00+0200"
   */
  public SearchHistoryRequest setFrom(String from) {
    this.from = from;
    return this;
  }

  public String getFrom() {
    return from;
  }

  /**
   * Comma-separated list of metric keys
   *
   * This is a mandatory parameter.
   * Example value: "ncloc,coverage,new_violations"
   */
  public SearchHistoryRequest setMetrics(List<String> metrics) {
    this.metrics = metrics;
    return this;
  }

  public List<String> getMetrics() {
    return metrics;
  }

  /**
   * 1-based page number
   *
   * Example value: "42"
   */
  public SearchHistoryRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Page size. Must be greater than 0 and less than 1000
   *
   * Example value: "20"
   */
  public SearchHistoryRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Filter measures created before the given date (inclusive). <br>Either a date (server timezone) or datetime can be provided
   *
   * Example value: "2017-10-19 or 2017-10-19T13:00:00+0200"
   */
  public SearchHistoryRequest setTo(String to) {
    this.to = to;
    return this;
  }

  public String getTo() {
    return to;
  }
}
