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
package org.sonarqube.ws.client.measures;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/component_tree">Further information about this action online (including a response example)</a>
 * @since 5.4
 */
@Generated("sonar-ws-generator")
public class ComponentTreeRequest {

  private List<String> additionalFields;
  private String asc;
  private String baseComponentId;
  private String branch;
  private String component;
  private String developerId;
  private String developerKey;
  private List<String> metricKeys;
  private String metricPeriodSort;
  private String metricSort;
  private String metricSortFilter;
  private String p;
  private String ps;
  private String pullRequest;
  private String q;
  private List<String> qualifiers;
  private List<String> s;
  private String strategy;

  /**
   * Example value: "periods,metrics"
   * Possible values:
   * <ul>
   *   <li>"metrics"</li>
   *   <li>"periods"</li>
   * </ul>
   */
  public ComponentTreeRequest setAdditionalFields(List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  public List<String> getAdditionalFields() {
    return additionalFields;
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
  public ComponentTreeRequest setAsc(String asc) {
    this.asc = asc;
    return this;
  }

  public String getAsc() {
    return asc;
  }

  /**
   * Example value: "AU-TpxcA-iU5OvuD2FLz"
   * @deprecated since 6.6
   */
  @Deprecated
  public ComponentTreeRequest setBaseComponentId(String baseComponentId) {
    this.baseComponentId = baseComponentId;
    return this;
  }

  public String getBaseComponentId() {
    return baseComponentId;
  }

  /**
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public ComponentTreeRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Example value: "my_project"
   */
  public ComponentTreeRequest setComponent(String component) {
    this.component = component;
    return this;
  }

  public String getComponent() {
    return component;
  }

  /**
   * @deprecated since 6.4
   */
  @Deprecated
  public ComponentTreeRequest setDeveloperId(String developerId) {
    this.developerId = developerId;
    return this;
  }

  public String getDeveloperId() {
    return developerId;
  }

  /**
   * @deprecated since 6.4
   */
  @Deprecated
  public ComponentTreeRequest setDeveloperKey(String developerKey) {
    this.developerKey = developerKey;
    return this;
  }

  public String getDeveloperKey() {
    return developerKey;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "ncloc,complexity,violations"
   */
  public ComponentTreeRequest setMetricKeys(List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  public List<String> getMetricKeys() {
    return metricKeys;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"1"</li>
   * </ul>
   */
  public ComponentTreeRequest setMetricPeriodSort(String metricPeriodSort) {
    this.metricPeriodSort = metricPeriodSort;
    return this;
  }

  public String getMetricPeriodSort() {
    return metricPeriodSort;
  }

  /**
   * Example value: "ncloc"
   */
  public ComponentTreeRequest setMetricSort(String metricSort) {
    this.metricSort = metricSort;
    return this;
  }

  public String getMetricSort() {
    return metricSort;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"all"</li>
   *   <li>"withMeasuresOnly"</li>
   * </ul>
   */
  public ComponentTreeRequest setMetricSortFilter(String metricSortFilter) {
    this.metricSortFilter = metricSortFilter;
    return this;
  }

  public String getMetricSortFilter() {
    return metricSortFilter;
  }

  /**
   * Example value: "42"
   */
  public ComponentTreeRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public ComponentTreeRequest setPs(String ps) {
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
  public ComponentTreeRequest setPullRequest(String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  public String getPullRequest() {
    return pullRequest;
  }

  /**
   * Example value: "FILE_NAM"
   */
  public ComponentTreeRequest setQ(String q) {
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
  public ComponentTreeRequest setQualifiers(List<String> qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public List<String> getQualifiers() {
    return qualifiers;
  }

  /**
   * Example value: "name,path"
   * Possible values:
   * <ul>
   *   <li>"metric"</li>
   *   <li>"metricPeriod"</li>
   *   <li>"name"</li>
   *   <li>"path"</li>
   *   <li>"qualifier"</li>
   * </ul>
   */
  public ComponentTreeRequest setS(List<String> s) {
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
  public ComponentTreeRequest setStrategy(String strategy) {
    this.strategy = strategy;
    return this;
  }

  public String getStrategy() {
    return strategy;
  }
}
