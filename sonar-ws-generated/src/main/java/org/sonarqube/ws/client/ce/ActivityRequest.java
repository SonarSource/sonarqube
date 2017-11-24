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
package org.sonarqube.ws.client.ce;

import java.util.List;
import javax.annotation.Generated;

/**
 * Search for tasks.<br> Requires the system administration permission, or project administration permission if componentId is set.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/activity">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class ActivityRequest {

  private String componentId;
  private String componentQuery;
  private String maxExecutedAt;
  private String minSubmittedAt;
  private String onlyCurrents;
  private String p;
  private String ps;
  private String q;
  private List<String> status;
  private String type;

  /**
   * Id of the component (project) to filter on
   *
   * Example value: "AU-TpxcA-iU5OvuD2FL0"
   */
  public ActivityRequest setComponentId(String componentId) {
    this.componentId = componentId;
    return this;
  }

  public String getComponentId() {
    return componentId;
  }

  /**
   * Limit search to: <ul><li>component names that contain the supplied string</li><li>component keys that are exactly the same as the supplied string</li></ul>Must not be set together with componentId.<br />Deprecated and replaced by 'q'
   *
   * Example value: "Apache"
   * @deprecated since 5.5
   */
  @Deprecated
  public ActivityRequest setComponentQuery(String componentQuery) {
    this.componentQuery = componentQuery;
    return this;
  }

  public String getComponentQuery() {
    return componentQuery;
  }

  /**
   * Maximum date of end of task processing (inclusive)
   *
   * Example value: "2017-11-23T15:56:03+0100"
   */
  public ActivityRequest setMaxExecutedAt(String maxExecutedAt) {
    this.maxExecutedAt = maxExecutedAt;
    return this;
  }

  public String getMaxExecutedAt() {
    return maxExecutedAt;
  }

  /**
   * Minimum date of task submission (inclusive)
   *
   * Example value: "2017-11-23T15:56:03+0100"
   */
  public ActivityRequest setMinSubmittedAt(String minSubmittedAt) {
    this.minSubmittedAt = minSubmittedAt;
    return this;
  }

  public String getMinSubmittedAt() {
    return minSubmittedAt;
  }

  /**
   * Filter on the last tasks (only the most recent finished task by project)
   *
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public ActivityRequest setOnlyCurrents(String onlyCurrents) {
    this.onlyCurrents = onlyCurrents;
    return this;
  }

  public String getOnlyCurrents() {
    return onlyCurrents;
  }

  /**
   * Deprecated parameter
   *
   * @deprecated since 5.5
   */
  @Deprecated
  public ActivityRequest setP(String p) {
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
  public ActivityRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Limit search to: <ul><li>component names that contain the supplied string</li><li>component keys that are exactly the same as the supplied string</li><li>task ids that are exactly the same as the supplied string</li></ul>Must not be set together with componentId
   *
   * Example value: "Apache"
   */
  public ActivityRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }

  /**
   * Comma separated list of task statuses
   *
   * Example value: "IN_PROGRESS,SUCCESS"
   * Possible values:
   * <ul>
   *   <li>"SUCCESS"</li>
   *   <li>"FAILED"</li>
   *   <li>"CANCELED"</li>
   *   <li>"PENDING"</li>
   *   <li>"IN_PROGRESS"</li>
   * </ul>
   */
  public ActivityRequest setStatus(List<String> status) {
    this.status = status;
    return this;
  }

  public List<String> getStatus() {
    return status;
  }

  /**
   * Task type
   *
   * Example value: "REPORT"
   * Possible values:
   * <ul>
   *   <li>"REPORT"</li>
   * </ul>
   */
  public ActivityRequest setType(String type) {
    this.type = type;
    return this;
  }

  public String getType() {
    return type;
  }
}
