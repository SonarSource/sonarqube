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
package org.sonarqube.ws.client.projectanalyses;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/search">Further information about this action online (including a response example)</a>
 * @since 6.3
 */
@Generated("sonar-ws-generator")
public class SearchRequest {

  private String branch;
  private String category;
  private String from;
  private String p;
  private String project;
  private String ps;
  private String to;

  /**
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public SearchRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Example value: "OTHER"
   * Possible values:
   * <ul>
   *   <li>"VERSION"</li>
   *   <li>"OTHER"</li>
   *   <li>"QUALITY_PROFILE"</li>
   *   <li>"QUALITY_GATE"</li>
   * </ul>
   */
  public SearchRequest setCategory(String category) {
    this.category = category;
    return this;
  }

  public String getCategory() {
    return category;
  }

  /**
   * Example value: "2013-05-01"
   */
  public SearchRequest setFrom(String from) {
    this.from = from;
    return this;
  }

  public String getFrom() {
    return from;
  }

  /**
   * Example value: "42"
   */
  public SearchRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "my_project"
   */
  public SearchRequest setProject(String project) {
    this.project = project;
    return this;
  }

  public String getProject() {
    return project;
  }

  /**
   * Example value: "20"
   */
  public SearchRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Example value: "2017-10-19 or 2017-10-19T13:00:00+0200"
   */
  public SearchRequest setTo(String to) {
    this.to = to;
    return this;
  }

  public String getTo() {
    return to;
  }
}
