/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

/**
 * @since 3.6
 */
public final class IssueQuery extends Query<Issue> {

  public static final String BASE_URL = "/api/issues";

  private String key;
  private String[] keys;
  private String[] severities;
  private String minSeverity;
  private String[] status;
  private String[] resolutions;
  private String[] components;
  private String[] rules;
  private String[] userLogins;
  private String[] assigneeLogins;
  private Integer limit;

  private IssueQuery() {
  }

  public static String getBaseUrl() {
    return BASE_URL;
  }

  public static IssueQuery create() {
    return new IssueQuery();
  }

  public static IssueQuery byKey(String key) {
    return new IssueQuery().setKey(key);
  }

  public String getKey() {
    return key;
  }

  public IssueQuery setKey(String key) {
    this.key = key;
    return this;
  }

  public String[] getKeys() {
    return keys;
  }

  public IssueQuery setKeys(String... keys) {
    this.keys = keys;
    return this;
  }

  public String[] getSeverities() {
    return severities;
  }

  public IssueQuery setSeverities(String... severities) {
    this.severities = severities;
    return this;
  }

  public String getMinSeverity() {
    return minSeverity;
  }

  public IssueQuery setMinSeverity(String minSeverity) {
    this.minSeverity = minSeverity;
    return this;
  }

  public String[] getStatus() {
    return status;
  }

  public IssueQuery setStatus(String... status) {
    this.status = status;
    return this;
  }

  public String[] getResolutions() {
    return resolutions;
  }

  public IssueQuery setResolutions(String... resolutions) {
    this.resolutions = resolutions;
    return this;
  }

  public String[] getComponents() {
    return components;
  }

  public IssueQuery setComponents(String... components) {
    this.components = components;
    return this;
  }

  public String[] getRules() {
    return rules;
  }

  public IssueQuery setRules(String... rules) {
    this.rules = rules;
    return this;
  }

  public String[] getUserLogins() {
    return userLogins;
  }

  public IssueQuery setUserLogins(String... userLogins) {
    this.userLogins = userLogins;
    return this;
  }

  public String[] getAssigneeLogins() {
    return assigneeLogins;
  }

  public IssueQuery setAssigneeLogins(String... assigneeLogins) {
    this.assigneeLogins = assigneeLogins;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public IssueQuery setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    if (key != null && !"".equals(key)) {
      url.append("/");
      url.append(encode(key));
    }
    url.append('?');
    appendUrlParameter(url, "keys", keys);
    appendUrlParameter(url, "severities", severities);
    appendUrlParameter(url, "minSeverity", minSeverity);
    appendUrlParameter(url, "status", status);
    appendUrlParameter(url, "resolutions", resolutions);
    appendUrlParameter(url, "components", components);
    appendUrlParameter(url, "rules", rules);
    appendUrlParameter(url, "userLogins", userLogins);
    appendUrlParameter(url, "assigneeLogins", assigneeLogins);
    appendUrlParameter(url, "limit", limit);
    return url.toString();
  }

  @Override
  public Class<Issue> getModelClass() {
    return Issue.class;
  }
}
