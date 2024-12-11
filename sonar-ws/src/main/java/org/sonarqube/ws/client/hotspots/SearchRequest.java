/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarqube.ws.client.hotspots;

import java.util.List;
import jakarta.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/search">Further information about this action online (including a response example)</a>
 * @since 8.1
 */
@Generated("sonar-ws-generator")
public class SearchRequest {

  private String branch;
  private List<String> hotspots;
  private String onlyMine;
  private String p;
  private String projectKey;
  private String ps;
  private String pullRequest;
  private String resolution;
  private String inNewCodePeriod;
  private String status;
  private String files;

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
   * Example value: "AWhXpLoInp4On-Y3xc8x"
   */
  public SearchRequest setHotspots(List<String> hotspots) {
    this.hotspots = hotspots;
    return this;
  }

  public List<String> getHotspots() {
    return hotspots;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchRequest setOnlyMine(String onlyMine) {
    this.onlyMine = onlyMine;
    return this;
  }

  public String getOnlyMine() {
    return onlyMine;
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
   * Example value: "my_project"
   */
  public SearchRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
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
   * This is part of the internal API.
   * Example value: "5461"
   */
  public SearchRequest setPullRequest(String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  public String getPullRequest() {
    return pullRequest;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"FIXED"</li>
   *   <li>"SAFE"</li>
   * </ul>
   */
  public SearchRequest setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public SearchRequest setInNewCodePeriod(String inNewCodePeriod) {
    this.inNewCodePeriod = inNewCodePeriod;
    return this;
  }

  public String getInNewCodePeriod() {
    return inNewCodePeriod;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"TO_REVIEW"</li>
   *   <li>"REVIEWED"</li>
   * </ul>
   */
  public SearchRequest setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public String getFiles() {
    return files;
  }

  public void setFiles(String files) {
    this.files = files;
  }
}
