/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

public class ResourceQuery extends Query<Resource> {
  public static final String BASE_URL = "/api/resources";

  public final static int DEPTH_UNLIMITED = -1;

  private Integer depth;
  private String resourceKeyOrId;
  private Integer limit;
  private String[] scopes;
  private String[] qualifiers;
  private String[] metrics;
  private String[] rules;
  private String[] ruleCategories;
  private String[] rulePriorities;
  private String[] characteristicKeys;
  private String model;
  private boolean excludeRules = true;
  private boolean excludeRuleCategories = true;
  private boolean excludeRulePriorities = true;
  private Boolean includeTrends = null;
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

  public ResourceQuery setCharacteristicKeys(String model, String... keys) {
    this.model=model;
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

  public String[] getRuleCategories() {
    return ruleCategories;
  }

  /**
   * @param ruleCategories values: Maintainability, Usability, Reliability, Efficiency, Portability
   */
  public ResourceQuery setRuleCategories(String... ruleCategories) {
    this.ruleCategories = ruleCategories;
    this.excludeRuleCategories = false;
    return this;
  }

  public String[] getRulePriorities() {
    return rulePriorities;
  }

  /**
   * @param rulePriorities values: BLOCKER, CRITICAL, MAJOR, MINOR, INFO
   */
  public ResourceQuery setRulePriorities(String... rulePriorities) {
    this.rulePriorities = rulePriorities;
    this.excludeRulePriorities = false;
    return this;
  }

  public boolean isExcludeRules() {
    return excludeRules;
  }

  public ResourceQuery setExcludeRules(boolean excludeRules) {
    this.excludeRules = excludeRules;
    return this;
  }

  public boolean isExcludeRuleCategories() {
    return excludeRuleCategories;
  }

  public ResourceQuery setExcludeRuleCategories(boolean excludeRuleCategories) {
    this.excludeRuleCategories = excludeRuleCategories;
    return this;
  }

  public boolean isExcludeRulePriorities() {
    return excludeRulePriorities;
  }

  public ResourceQuery setExcludeRulePriorities(boolean excludeRulePriorities) {
    this.excludeRulePriorities = excludeRulePriorities;
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
    appendRuleField(url, "rule_categories", excludeRuleCategories, ruleCategories);
    appendRuleField(url, "rule_priorities", excludeRulePriorities, rulePriorities);
    appendUrlParameter(url, "includetrends", includeTrends);
    appendUrlParameter(url, "model", model);
    appendUrlParameter(url, "characteristics", characteristicKeys);
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
    return new ResourceQuery(resource.getId().toString())
        .setMetrics(metricKeys);
  }
}
