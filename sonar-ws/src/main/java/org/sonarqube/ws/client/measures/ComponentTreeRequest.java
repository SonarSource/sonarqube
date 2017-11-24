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
 * Navigate through components based on the chosen strategy with specified measures. The baseComponentId or the component parameter must be provided.<br>Requires the following permission: 'Browse' on the specified project.<br>When limiting search with the q parameter, directories are not returned.
 *
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
  private String metricKeys;
  private String metricPeriodSort;
  private String metricSort;
  private String metricSortFilter;
  private String p;
  private String ps;
  private String q;
  private List<String> qualifiers;
  private List<String> s;
  private String strategy;

  /**
   * Comma-separated list of additional fields that can be returned in the response.
   *
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
  public ComponentTreeRequest setAsc(String asc) {
    this.asc = asc;
    return this;
  }

  public String getAsc() {
    return asc;
  }

  /**
   * Base component id. The search is based on this component.
   *
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
   * Branch key
   *
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
   * Component key. The search is based on this component.
   *
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
   * Deprecated parameter, used previously with the Developer Cockpit plugin. No measures are returned if parameter is set.
   *
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
   * Deprecated parameter, used previously with the Developer Cockpit plugin. No measures are returned if parameter is set.
   *
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
   * Metric keys. Types DISTRIB, DATA are not allowed
   *
   * This is a mandatory parameter.
   * Example value: "ncloc,complexity,violations"
   */
  public ComponentTreeRequest setMetricKeys(String metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  public String getMetricKeys() {
    return metricKeys;
  }

  /**
   * Sort measures by leak period or not ?. The 's' parameter must contain the 'metricPeriod' value.
   *
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
   * Metric key to sort by. The 's' parameter must contain the 'metric' or 'metricPeriod' value. It must be part of the 'metricKeys' parameter
   *
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
   * Filter components. Sort must be on a metric. Possible values are: <ul><li>all: return all components</li><li>withMeasuresOnly: filter out components that do not have a measure on the sorted metric</li></ul>
   *
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
   * 1-based page number
   *
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
   * Page size. Must be greater than 0 and less than 500
   *
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
   * Limit search to: <ul><li>component names that contain the supplied string</li><li>component keys that are exactly the same as the supplied string</li></ul>
   *
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
   * Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. Possible values are:<ul><li>BRC - Sub-projects</li><li>DIR - Directories</li><li>FIL - Files</li><li>TRK - Projects</li><li>UTS - Test Files</li></ul>
   *
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
   * Comma-separated list of sort fields
   *
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
   * Strategy to search for base component descendants:<ul><li>children: return the children components of the base component. Grandchildren components are not returned</li><li>all: return all the descendants components of the base component. Grandchildren are returned.</li><li>leaves: return all the descendant components (files, in general) which don't have other children. They are the leaves of the component tree.</li></ul>
   *
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
