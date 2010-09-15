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

public class ViolationQuery extends Query<Violation> {
  public static final String BASE_URL = "/api/violations";

  private String[] scopes;
  private String[] qualifiers;
  private String[] ruleKeys;
  private String[] categories;
  private String[] priorities;
  private int depth = 0;
  private String resourceKeyOrId;

  public ViolationQuery(String resourceKeyOrId) {
    this.resourceKeyOrId = resourceKeyOrId;
  }

  public String[] getScopes() {
    return scopes;
  }

  public ViolationQuery setScopes(String... scopes) {
    this.scopes = scopes;
    return this;
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

  public String[] getCategories() {
    return categories;
  }

  public ViolationQuery setCategories(String... categories) {
    this.categories = categories;
    return this;
  }

  public String[] getPriorities() {
    return priorities;
  }

  public ViolationQuery setPriorities(String... priorities) {
    this.priorities = priorities;
    return this;
  }

  public int getDepth() {
    return depth;
  }

  public ViolationQuery setDepth(int depth) {
    this.depth = depth;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append("?resource=")
        .append(resourceKeyOrId)
        .append("&");

    if (depth != 0) {
      url.append("depth=").append(depth).append("&");
    }
    append(url, "scopes", scopes);
    append(url, "qualifiers", qualifiers);
    append(url, "rules", ruleKeys);
    append(url, "categories", categories);
    append(url, "priorities", priorities);
    return url.toString();
  }

  private void append(StringBuilder url, String paramKey, Object[] paramValues) {
    if (paramValues != null && paramValues.length > 0) {
      url.append(paramKey).append('=');
      for (int index = 0; index < paramValues.length; index++) {
        if (index > 0) {
          url.append(',');
        }
        url.append(paramValues[index]);
      }
      url.append('&');
    }
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
