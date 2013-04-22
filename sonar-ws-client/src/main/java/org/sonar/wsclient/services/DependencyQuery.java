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
 * The web service "dependency" is since Sonar 2.0
 */
public class DependencyQuery extends Query<Dependency> {
  public static final String BASE_URL = "/api/dependencies";

  private String resourceIdOrKey = null;
  private String direction = null;
  private String parentId = null;
  private String id = null;
  public static final String INCOMING_DIRECTION = "in";
  public static final String OUTGOING_DIRECTION = "out";

  public String getResourceIdOrKey() {
    return resourceIdOrKey;
  }

  public DependencyQuery setResourceIdOrKey(String resourceIdOrKey) {
    this.resourceIdOrKey = resourceIdOrKey;
    return this;
  }

  public DependencyQuery setResourceId(long resourceId) {
    this.resourceIdOrKey = String.valueOf(resourceId);
    return this;
  }

  public String getDirection() {
    return direction;
  }

  public DependencyQuery setDirection(String direction) {
    this.direction = direction;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "resource", resourceIdOrKey);
    appendUrlParameter(url, "dir", direction);
    appendUrlParameter(url, "parent", parentId);
    appendUrlParameter(url, "id", id);
    return url.toString();
  }

  public String getParentId() {
    return parentId;
  }

  public String getId() {
    return id;
  }

  public DependencyQuery setId(String id) {
    this.id = id;
    return this;
  }

  public DependencyQuery setParentId(String parentId) {
    this.parentId = parentId;
    return this;
  }

  @Override
  public Class<Dependency> getModelClass() {
    return Dependency.class;
  }

  /**
   * Resources that depend upon a resource
   * 
   * @param resourceIdOrKey the target resource. Can be the primary key (a number) or the logical key (String)
   */
  public static DependencyQuery createForIncomingDependencies(String resourceIdOrKey) {
    DependencyQuery query = new DependencyQuery();
    query.setResourceIdOrKey(resourceIdOrKey);
    query.setDirection(INCOMING_DIRECTION);
    return query;
  }

  /**
   * Resources that are depended upon a resource = all the resources that a resource depends upon
   * 
   * @param resourceIdOrKey the target resource. Can be the primary key (an integer) or the logical key (String)
   */
  public static DependencyQuery createForOutgoingDependencies(String resourceIdOrKey) {
    DependencyQuery query = new DependencyQuery();
    query.setResourceIdOrKey(resourceIdOrKey);
    query.setDirection(OUTGOING_DIRECTION);
    return query;
  }

  /**
   * Resources that depend upon or are depended upon a resource. It equals the merge of createForIncomingDependencies(resourceIdOrKey)
   * and createForOutgoingDependencies(resourceIdOrKey)
   * 
   * @param resourceIdOrKey the target resource. Can be the primary key (an integer) or the logical key (String)
   */
  public static DependencyQuery createForResource(String resourceIdOrKey) {
    DependencyQuery query = new DependencyQuery();
    query.setResourceIdOrKey(resourceIdOrKey);
    return query;
  }

  public static DependencyQuery createForResource(long resourceId) {
    DependencyQuery query = new DependencyQuery();
    query.setResourceId(resourceId);
    return query;
  }

  public static DependencyQuery createForSubDependencies(String dependencyId) {
    DependencyQuery query = new DependencyQuery();
    query.setParentId(dependencyId);
    return query;
  }

  public static DependencyQuery createForId(String id) {
    DependencyQuery query = new DependencyQuery();
    query.setId(id);
    return query;
  }
}
