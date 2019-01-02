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
package org.sonarqube.ws.client.components;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/tree">Further information about this action online (including a response example)</a>
 * @since 5.4
 */
@Generated("sonar-ws-generator")
public class TreeRequest {

  private String asc;
  private String branch;
  private String component;
  private String componentId;
  private String p;
  private String ps;
  private String pullRequest;
  private String q;
  private List<String> qualifiers;
  private List<String> s;
  private String strategy;

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public TreeRequest setAsc(String asc) {
    this.asc = asc;
    return this;
  }

  public String getAsc() {
    return asc;
  }

  /**
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public TreeRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Example value: "my_project"
   */
  public TreeRequest setComponent(String component) {
    this.component = component;
    return this;
  }

  public String getComponent() {
    return component;
  }

  /**
   * Example value: "AU-TpxcA-iU5OvuD2FLz"
   * @deprecated since 6.4
   */
  @Deprecated
  public TreeRequest setComponentId(String componentId) {
    this.componentId = componentId;
    return this;
  }

  public String getComponentId() {
    return componentId;
  }

  /**
   * Example value: "42"
   */
  public TreeRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public TreeRequest setPs(String ps) {
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
  public TreeRequest setPullRequest(String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  public String getPullRequest() {
    return pullRequest;
  }

  /**
   * Example value: "FILE_NAM"
   */
  public TreeRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"BRC"</li>
   *   <li>"DIR"</li>
   *   <li>"FIL"</li>
   *   <li>"TRK"</li>
   *   <li>"UTS"</li>
   * </ul>
   */
  public TreeRequest setQualifiers(List<String> qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public List<String> getQualifiers() {
    return qualifiers;
  }

  /**
   * Example value: "name, path"
   * Possible values:
   * <ul>
   *   <li>"name"</li>
   *   <li>"path"</li>
   *   <li>"qualifier"</li>
   * </ul>
   */
  public TreeRequest setS(List<String> s) {
    this.s = s;
    return this;
  }

  public List<String> getS() {
    return s;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"all"</li>
   *   <li>"children"</li>
   *   <li>"leaves"</li>
   * </ul>
   */
  public TreeRequest setStrategy(String strategy) {
    this.strategy = strategy;
    return this;
  }

  public String getStrategy() {
    return strategy;
  }
}
