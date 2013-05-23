/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

/**
 * @deprecated in 3.6. Replaced by issues.
 */
@Deprecated
public class ViolationQuery extends Query<Violation> {

  public static final String BASE_URL = "/api/violations";

  private String resourceKeyOrId;
  private int depth = 0;
  private String[] qualifiers;
  private String[] ruleKeys;
  private String[] severities;
  private Integer limit;

  public ViolationQuery(String resourceKeyOrId) {
    this.resourceKeyOrId = resourceKeyOrId;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  public ViolationQuery setQualifiers(String... qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public String[] getRuleKeys() {
    return ruleKeys;
  }

  public ViolationQuery setRuleKeys(String... ruleKeys) {
    this.ruleKeys = ruleKeys;
    return this;
  }

  /**
   * @since 2.5
   */
  public String[] getSeverities() {
    return severities;
  }

  /**
   * @since 2.5
   */
  public ViolationQuery setSeverities(String... severities) {
    this.severities = severities;
    return this;
  }

  /**
   * @deprecated since 2.5 use {@link #getSeverities()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public String[] getPriorities() {
    return severities;
  }

  /**
   * @deprecated since 2.5 use {@link #setSeverities(String...)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public ViolationQuery setPriorities(String... priorities) {
    this.severities = priorities;
    return this;
  }

  public int getDepth() {
    return depth;
  }

  public ViolationQuery setDepth(int depth) {
    this.depth = depth;
    return this;
  }

  /**
   * @since 2.5
   */
  public Integer getLimit() {
    return limit;
  }

  /**
   * @since 2.5
   */
  public ViolationQuery setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "resource", resourceKeyOrId);
    if (depth != 0) {
      url.append("depth=").append(depth).append("&");
    }
    appendUrlParameter(url, "limit", limit);
    appendUrlParameter(url, "qualifiers", qualifiers);
    appendUrlParameter(url, "rules", ruleKeys);
    appendUrlParameter(url, "priorities", severities);
      return url.toString();
  }

  @Override
  public Class<Violation> getModelClass() {
    return Violation.class;
  }

  public static ViolationQuery createForResource(Resource resource) {
    return new ViolationQuery(resource.getId().toString());
  }

  public static ViolationQuery createForResource(String resourceIdOrKey) {
    return new ViolationQuery(resourceIdOrKey);
  }
}
