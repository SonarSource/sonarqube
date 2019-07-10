/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/activity">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class ActivityRequest {

  private String component;
  private String componentId;
  private String maxExecutedAt;
  private String minSubmittedAt;
  private String onlyCurrents;
  private String ps;
  private String q;
  private List<String> status;
  private String type;

  /**
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
   * Example value: "sample:src/main/xoo/sample/Sample2.xoo"
   */
  public ActivityRequest setComponent(String component) {
    this.component = component;
    return this;
  }

  public String getComponent() {
    return component;
  }

  /**
   * Example value: "2017-10-19T13:00:00+0200"
   */
  public ActivityRequest setMaxExecutedAt(String maxExecutedAt) {
    this.maxExecutedAt = maxExecutedAt;
    return this;
  }

  public String getMaxExecutedAt() {
    return maxExecutedAt;
  }

  /**
   * Example value: "2017-10-19T13:00:00+0200"
   */
  public ActivityRequest setMinSubmittedAt(String minSubmittedAt) {
    this.minSubmittedAt = minSubmittedAt;
    return this;
  }

  public String getMinSubmittedAt() {
    return minSubmittedAt;
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
  public ActivityRequest setOnlyCurrents(String onlyCurrents) {
    this.onlyCurrents = onlyCurrents;
    return this;
  }

  public String getOnlyCurrents() {
    return onlyCurrents;
  }

  /**
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
