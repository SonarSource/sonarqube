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
package org.sonar.core.resource;

import java.util.Date;

public final class SnapshotDto {
  private Long id;
  private Long parentId;
  private Long rootId;
  
  private Date date;
  private Date buildDate;
  private Long resourceId;
  private String status;
  private Integer purgeStatus;
  private Boolean last;
  private String scope;
  private String qualifier;
  private String version;
  private String path;
  private Integer depth;
  private Long rootProjectId;

  public Long getId() {
    return id;
  }

  public SnapshotDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getParentId() {
    return parentId;
  }

  public SnapshotDto setParentId(Long parentId) {
    this.parentId = parentId;
    return this;
  }

  public Long getRootId() {
    return rootId;
  }

  public SnapshotDto setRootId(Long rootId) {
    this.rootId = rootId;
    return this;
  }

  public Date getDate() {
    return date;//NOSONAR May expose internal representation by returning reference to mutable object
  }

  public SnapshotDto setDate(Date date) {
    this.date = date;// NOSONAR May expose internal representation by incorporating reference to mutable object
    return this;
  }

  public Date getBuildDate() {
    return buildDate;//NOSONAR May expose internal representation by returning reference to mutable object
  }

  public SnapshotDto setBuildDate(Date buildDate) {
    this.buildDate = buildDate;// NOSONAR May expose internal representation by incorporating reference to mutable object
    return this;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public SnapshotDto setResourceId(Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public SnapshotDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public Integer getPurgeStatus() {
    return purgeStatus;
  }

  public SnapshotDto setPurgeStatus(Integer purgeStatus) {
    this.purgeStatus = purgeStatus;
    return this;
  }

  public Boolean getLast() {
    return last;
  }

  public SnapshotDto setLast(Boolean last) {
    this.last = last;
    return this;
  }

  public String getScope() {
    return scope;
  }

  public SnapshotDto setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public SnapshotDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public SnapshotDto setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getPath() {
    return path;
  }

  public SnapshotDto setPath(String path) {
    this.path = path;
    return this;
  }

  public Integer getDepth() {
    return depth;
  }

  public SnapshotDto setDepth(Integer depth) {
    this.depth = depth;
    return this;
  }

  public Long getRootProjectId() {
    return rootProjectId;
  }

  public SnapshotDto setRootProjectId(Long rootProjectId) {
    this.rootProjectId = rootProjectId;
    return this;
  }
}
