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
package org.sonar.api.database.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.database.DatabaseSession;

import javax.persistence.*;
import java.util.Date;

/**
 * A class to map a snapshot with its hibernate model
 */
@Entity
@Table(name = "snapshots")
public class Snapshot extends BaseIdentifiable {

  /**
   * This status is set on the snapshot at the beginning of the batch
   */
  public final static String STATUS_UNPROCESSED = "U";

  /**
   * This status is set on the snapshot at the end of the batch
   */
  public final static String STATUS_PROCESSED = "P";

  @Column(name = "project_id", updatable = true, nullable = true)
  private Integer resourceId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at", updatable = true, nullable = true)
  private Date createdAt;

  @Column(name = "version", updatable = true, nullable = true, length = 60)
  private String version;

  @Column(name = "islast")
  private Boolean last = Boolean.FALSE;

  @Column(name = "status")
  private String status = STATUS_UNPROCESSED;

  @Column(name = "scope", updatable = true, nullable = true, length = 3)
  private String scope;

  @Column(name = "path", updatable = true, nullable = true, length = 96)
  private String path;

  @Column(name = "depth", updatable = true, nullable = true)
  private Integer depth;

  @Column(name = "qualifier", updatable = true, nullable = true, length = 3)
  private String qualifier;

  @Column(name = "root_snapshot_id", updatable = true, nullable = true)
  private Integer rootId;

  @Column(name = "parent_snapshot_id", updatable = true, nullable = true)
  private Integer parentId;

  @Column(name = "root_project_id", updatable = true, nullable = true)
  private Integer rootProjectId;

  @Column(name = "var_mode_1", updatable = true, nullable = true, length = 100)
  private String varMode1;

  @Column(name = "var_mode_2", updatable = true, nullable = true, length = 100)
  private String varMode2;

  @Column(name = "var_mode_3", updatable = true, nullable = true, length = 100)
  private String varMode3;

  @Column(name = "var_label_1", updatable = true, nullable = true, length = 100)
  private String varLabel1;

  @Column(name = "var_label_2", updatable = true, nullable = true, length = 100)
  private String varLabel2;

  @Column(name = "var_label_3", updatable = true, nullable = true, length = 100)
  private String varLabel3;

  public Snapshot() {

  }

  public Snapshot(ResourceModel resource, Snapshot parent) {
    this.resourceId = resource.getId();
    this.qualifier = resource.getQualifier();
    this.scope = resource.getScope();
    
    if (parent == null) {
      path = "";
      depth = 0;
      this.createdAt = new Date();

    } else {
      this.parentId = parent.getId();
      this.rootId = (parent.getRootId() == null ? parent.getId() : parent.getRootId());
      this.createdAt = parent.getCreatedAt();
      this.depth = parent.getDepth() + 1;
      this.path = new StringBuilder()
          .append(parent.getPath())
          .append(parent.getId())
          .append(".")
          .toString();
    }
    this.rootProjectId = guessRootProjectId(resource, parent);
  }

  private static Integer guessRootProjectId(ResourceModel resource, Snapshot parent) {
    Integer result;

    // design problem : constants are defined in the Resource class, that should not be used by this class...
    if ("TRK".equals(resource.getQualifier()) || "VW".equals(resource.getQualifier()) || "SVW".equals(resource.getQualifier())) {
      result = resource.getCopyResourceId() != null ? resource.getCopyResourceId() : resource.getId();

    } else if (parent == null) {
      result = resource.getCopyResourceId() != null ? resource.getCopyResourceId() : resource.getId();

    } else {
      result = (parent.getRootProjectId() == null ? parent.getResourceId() : parent.getRootProjectId());
    }
    return result;
  }

  public Snapshot save(DatabaseSession session) {
    return session.save(this);
  }

  public Snapshot(ResourceModel resource, boolean last, String status, Date date) {
    this();
    setResource(resource);
    this.status = status;
    this.last = last;
    this.createdAt = date;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public Snapshot setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public Snapshot setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public final Snapshot setResource(ResourceModel resource) {
    this.resourceId = resource.getId();
    this.scope = resource.getScope();
    this.qualifier = resource.getQualifier();
    return this;
  }

  public String getVersion() {
    return version;
  }

  public Snapshot setVersion(String version) {
    this.version = version;
    return this;
  }

  public Integer getParentId() {
    return parentId;
  }

  public Snapshot setParentId(Integer i) {
    this.parentId = i;
    return this;
  }

  public Boolean getLast() {
    return last;
  }

  public Snapshot setLast(Boolean last) {
    this.last = last;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public Snapshot setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getScope() {
    return scope;
  }

  public Snapshot setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public Snapshot setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public Integer getRootId() {
    return rootId;
  }

  public Snapshot setRootId(Integer i) {
    this.rootId = i;
    return this;
  }

  public String getPath() {
    return path;
  }

  public Snapshot setPath(String path) {
    this.path = path;
    return this;
  }

  public Integer getDepth() {
    return depth;
  }

  public Integer getRootProjectId() {
    return rootProjectId;
  }

  public Snapshot setRootProjectId(Integer rootProjectId) {
    this.rootProjectId = rootProjectId;
    return this;
  }

  /**
   * Sets the depth of the snapshot
   *
   * @throws IllegalArgumentException when depth is negative
   */
  public void setDepth(Integer depth) {
    if (depth != null && depth < 0) {
      throw new IllegalArgumentException("depth can not be negative : " + depth);
    }
    this.depth = depth;
  }

  public String getVarMode1() {
    return varMode1;
  }

  public Snapshot setVarMode1(String varMode1) {
    this.varMode1 = varMode1;
    return this;
  }

  public String getVarMode2() {
    return varMode2;
  }

  public Snapshot setVarMode2(String varMode2) {
    this.varMode2 = varMode2;
    return this;
  }

  public String getVarMode3() {
    return varMode3;
  }

  public Snapshot setVarMode3(String varMode3) {
    this.varMode3 = varMode3;
    return this;
  }

  public String getVarLabel1() {
    return varLabel1;
  }

  public Snapshot setVarLabel1(String varLabel1) {
    this.varLabel1 = varLabel1;
    return this;
  }

  public String getVarLabel2() {
    return varLabel2;
  }

  public Snapshot setVarLabel2(String varLabel2) {
    this.varLabel2 = varLabel2;
    return this;
  }

  public String getVarLabel3() {
    return varLabel3;
  }

  public Snapshot setVarLabel3(String varLabel3) {
    this.varLabel3 = varLabel3;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Snapshot)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Snapshot other = (Snapshot) obj;
    return new EqualsBuilder()
        .append(resourceId, other.getResourceId())
        .append(createdAt, other.getCreatedAt())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(resourceId)
        .append(createdAt)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("resourceId", resourceId)
        .append("scope", scope)
        .append("qualifier", qualifier)
        .append("version", version)
        .append("last", last)
        .append("createdAt", createdAt)
        .append("status", status)
        .append("path", path)
        .append("rootId", rootId)
        .append("rootProjectId", rootProjectId)
        .append("parentId", parentId)
        .append("varMode1", varMode1)
        .append("varLabel1", varLabel1)
        .append("varMode2", varMode2)
        .append("varLabel2", varLabel2)
        .append("varMode3", varMode3)
        .append("varLabel3", varLabel3)
        .toString();
  }
}
