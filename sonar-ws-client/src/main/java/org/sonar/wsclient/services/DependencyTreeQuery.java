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

/**
 * @since 2.1
 */
public class DependencyTreeQuery extends Query<DependencyTree> {
  private static final String BASE_URL = "/api/dependency_tree";

  private String resourceId;
  private String[] scopes;

  public DependencyTreeQuery(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String[] getScopes() {
    return scopes;
  }

  public DependencyTreeQuery setResourceId(String resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public DependencyTreeQuery setScopes(String... scopes) {
    this.scopes = scopes;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append("?resource=")
        .append(resourceId)
        .append("&");
    if (scopes != null) {
      url.append("scopes=");
      if (scopes != null) {
        for (int index = 0; index < scopes.length; index++) {
          if (index > 0) {
            url.append(',');
          }
          if (scopes[index] != null) {
            url.append(scopes[index]);
          }
        }
        url.append('&');
      }
    }
    return url.toString();
  }

  @Override
  public Class<DependencyTree> getModelClass() {
    return DependencyTree.class;
  }

  public static DependencyTreeQuery createForResource(String resourceIdOrKey) {
    return new DependencyTreeQuery(resourceIdOrKey);
  }

  public static DependencyTreeQuery createForProject(String projectIdOrKey) {
    return new DependencyTreeQuery(projectIdOrKey).setScopes(Resource.SCOPE_SET);
  }
}
