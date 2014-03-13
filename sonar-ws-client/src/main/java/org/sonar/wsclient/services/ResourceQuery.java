/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.services;

public class ResourceQuery extends Query<Resource> {

  public static final String BASE_URL = "/api/resources";

  public static final int DEPTH_UNLIMITED = -1;

  private Integer depth;
  private String resourceKeyOrId;
  private Integer limit;
  private String[] scopes;
  private String[] qualifiers;
  private String[] metrics;
  private String[] rules;
  private String[] ruleSeverities;
  private String[] characteristicKeys;
  private boolean excludeRules = true;
  private boolean excludeRuleSeverities = true;
  private Boolean includeTrends = null;
  private Boolean includeAlerts = null;
  private Boolean verbose = Boolean.FALSE;

  public ResourceQuery() {
  }

  public ResourceQuery(String resourceKeyOrId) {
    this.resourceKeyOrId = resourceKeyOrId;
  }

  public ResourceQuery(long resourceId) {
    this.resourceKeyOrId = String.valueOf(resourceId);
  }

  public Integer getDepth() {
    return depth;
  }

  public ResourceQuery setDepth(Integer depth) {
    this.depth = depth;
    return this;
  }

  public ResourceQuery setAllDepths() {
    return setDepth(DEPTH_UNLIMITED);
  }

  public String getResourceKeyOrId() {
    return resourceKeyOrId;
  }

  public ResourceQuery setResourceKeyOrId(String resourceKeyOrId) {
    this.resourceKeyOrId = resourceKeyOrId;
    return this;
  }

  public ResourceQuery setResourceId(int resourceId) {
    this.resourceKeyOrId = Integer.toString(resourceId);
    return this;
  }

  public ResourceQuery setCharacteristics(String... keys) {
    this.characteristicKeys = keys;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public ResourceQuery setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public String[] getScopes() {
    return scopes;
  }

  public ResourceQuery setScopes(String... scopes) {
    this.scopes = scopes;
    return this;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  public ResourceQuery setQualifiers(String... qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public String[] getMetrics() {
    return metrics;
  }

  public ResourceQuery setMetrics(String... metrics) {
    this.metrics = metrics;
    return this;
  }

  public String[] getRules() {
    return rules;
  }

  public ResourceQuery setRules(String... rules) {
    this.rules = rules;
    this.excludeRules = false;
    return this;
  }

  /**
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public String[] getRuleCategories() {
    return null;
  }

  /**
   * @param ruleCategories values: Maintainability, Usability, Reliability, Efficiency, Portability
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public ResourceQuery setRuleCategories(String... ruleCategories) {
    return this;
  }

  /**
   * @since 2.5
   */
  public String[] getRuleSeverities() {
    return ruleSeverities;
  }

  /**
   * @since 2.5
   * @param ruleSeverities values: BLOCKER, CRITICAL, MAJOR, MINOR, INFO
   */
  public ResourceQuery setRuleSeverities(String... ruleSeverities) {
    this.ruleSeverities = ruleSeverities;
    this.excludeRuleSeverities = false;
    return this;
  }

  /**
   * @deprecated since 2.5 use {@link #getRuleSeverities()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public String[] getRulePriorities() {
    return ruleSeverities;
  }

  /**
   * @deprecated since 2.5 use {@link #setRuleSeverities(String...)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public ResourceQuery setRulePriorities(String... rulePriorities) {
    return setRuleSeverities(rulePriorities);
  }

  public boolean isExcludeRules() {
    return excludeRules;
  }

  public ResourceQuery setExcludeRules(boolean excludeRules) {
    this.excludeRules = excludeRules;
    return this;
  }

  /**
   * @deprecated since 2.5 not used anymore
   */
  @Deprecated
  public boolean isExcludeRuleCategories() {
    return false;
  }

  /**
   * @deprecated since 2.5 not used anymore
   */
  @Deprecated
  public ResourceQuery setExcludeRuleCategories(boolean b) {
    return this;
  }

  /**
   * @since 2.5
   */
  public boolean isExcludeRuleSeverities() {
    return excludeRuleSeverities;
  }

  public ResourceQuery setExcludeRuleSeverities(boolean excludeRuleSeverities) {
    this.excludeRuleSeverities = excludeRuleSeverities;
    return this;
  }

  /**
   * @deprecated since 2.5 use {@link #isExcludeRuleSeverities()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public boolean isExcludeRulePriorities() {
    return excludeRuleSeverities;
  }

  /**
   * @deprecated since 2.5 use {@link #setExcludeRuleSeverities(boolean)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public ResourceQuery setExcludeRulePriorities(boolean b) {
    this.excludeRuleSeverities = b;
    return this;
  }

  public Boolean isVerbose() {
    return verbose;
  }

  public ResourceQuery setVerbose(Boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  public Boolean isIncludeTrends() {
    return includeTrends;
  }

  public ResourceQuery setIncludeTrends(Boolean includeTrends) {
    this.includeTrends = includeTrends;
    return this;
  }

  public Boolean isIncludeAlerts() {
    return includeAlerts;
  }

  public ResourceQuery setIncludeAlerts(Boolean includeAlerts) {
    this.includeAlerts = includeAlerts;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "resource", resourceKeyOrId);
    appendUrlParameter(url, "metrics", metrics);
    appendUrlParameter(url, "scopes", scopes);
    appendUrlParameter(url, "qualifiers", qualifiers);
    appendUrlParameter(url, "depth", depth);
    appendUrlParameter(url, "limit", limit);
    appendRuleField(url, "rules", excludeRules, rules);
    appendRuleField(url, "rule_priorities", excludeRuleSeverities, ruleSeverities);
    appendUrlParameter(url, "includetrends", includeTrends);
    appendUrlParameter(url, "characteristics", characteristicKeys);
    appendUrlParameter(url, "includealerts", includeAlerts);
    appendUrlParameter(url, "verbose", verbose);
    return url.toString();
  }

  private void appendRuleField(StringBuilder url, String field, boolean excludeField, String[] list) {
    if (!excludeField) {
      if (list == null || list.length == 0) {
        appendUrlParameter(url, field, true);
      } else {
        appendUrlParameter(url, field, list);
      }
    }
  }

  @Override
  public final Class<Resource> getModelClass() {
    return Resource.class;
  }

  public static ResourceQuery createForMetrics(String resourceKeyOrId, String... metricKeys) {
    return new ResourceQuery(resourceKeyOrId)
        .setMetrics(metricKeys);
  }

  public static ResourceQuery createForResource(Resource resource, String... metricKeys) {
    Integer id = resource.getId();
    if (id == null) {
      throw new IllegalArgumentException("id must be set");
    }
    return new ResourceQuery(id.toString())
        .setMetrics(metricKeys);
  }

  /**
   * @since 2.10
   */
  public static ResourceQuery create(String resourceKey) {
    return new ResourceQuery(resourceKey);
  }
}
