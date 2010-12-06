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
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
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

  @Column(name = "variation_mode_1", updatable = true, nullable = true, length = 100)
  private String variationMode1;

  @Column(name = "variation_mode_2", updatable = true, nullable = true, length = 100)
  private String variationMode2;

  @Column(name = "variation_mode_3", updatable = true, nullable = true, length = 100)
  private String variationMode3;

  @Column(name = "variation_mode_4", updatable = true, nullable = true, length = 100)
  private String variationMode4;

  @Column(name = "variation_mode_5", updatable = true, nullable = true, length = 100)
  private String variationMode5;

  @Column(name = "variation_param_1", updatable = true, nullable = true, length = 100)
  private String variationModeParam1;

  @Column(name = "variation_param_2", updatable = true, nullable = true, length = 100)
  private String variationModeParam2;

  @Column(name = "variation_param_3", updatable = true, nullable = true, length = 100)
  private String variationModeParam3;

  @Column(name = "variation_param_4", updatable = true, nullable = true, length = 100)
  private String variationModeParam4;

  @Column(name = "variation_param_5", updatable = true, nullable = true, length = 100)
  private String variationModeParam5;

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

  public String getVariationMode1() {
    return variationMode1;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationMode1(String s) {
    this.variationMode1 = s;
    return this;
  }

  public String getVariationMode2() {
    return variationMode2;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationMode2(String s) {
    this.variationMode2 = s;
    return this;
  }

  public String getVariationMode3() {
    return variationMode3;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationMode3(String s) {
    this.variationMode3 = s;
    return this;
  }

  public String getVariationMode4() {
    return variationMode4;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationMode4(String s) {
    this.variationMode4 = s;
    return this;
  }

  public String getVariationMode5() {
    return variationMode5;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationMode5(String s) {
    this.variationMode5 = s;
    return this;
  }

  public String getVariationModeParam1() {
    return variationModeParam1;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationModeParam1(String s) {
    this.variationModeParam1 = s;
    return this;
  }

  public String getVariationModeParam2() {
    return variationModeParam2;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationModeParam2(String s) {
    this.variationModeParam2 = s;
    return this;
  }

  public String getVariationModeParam3() {
    return variationModeParam3;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationModeParam3(String s) {
    this.variationModeParam3 = s;
    return this;
  }

  public String getVariationModeParam4() {
    return variationModeParam4;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationModeParam4(String s) {
    this.variationModeParam4 = s;
    return this;
  }

  public String getVariationModeParam5() {
    return variationModeParam5;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationModeParam5(String s) {
    this.variationModeParam5 = s;
    return this;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationMode(int index, String s) {
    switch(index) {
      case 1: variationMode1 = s; break;
      case 2: variationMode2 = s; break;
      case 3: variationMode3 = s; break;
      case 4: variationMode4 = s; break;
      case 5: variationMode5 = s; break;
      default: throw new IndexOutOfBoundsException("Index of Snapshot.variationMode is between 1 and 5");
    }
    return this;
  }

  public String getVariationMode(int index) {
    switch(index) {
      case 1: return variationMode1;
      case 2: return variationMode2;
      case 3: return variationMode3;
      case 4: return variationMode4;
      case 5: return variationMode5;
      default: throw new IndexOutOfBoundsException("Index of Snapshot.variationMode is between 1 and 5");
    }
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setVariationModeParam(int index, String s) {
    switch(index) {
      case 1: variationModeParam1 = s; break;
      case 2: variationModeParam2 = s; break;
      case 3: variationModeParam3 = s; break;
      case 4: variationModeParam4 = s; break;
      case 5: variationModeParam5 = s; break;
      default: throw new IndexOutOfBoundsException("Index of Snapshot.variationModeParam is between 1 and 5");
    }
    return this;
  }

  public String getVariationModeParam(int index) {
    switch(index) {
      case 1: return variationModeParam1;
      case 2: return variationModeParam2;
      case 3: return variationModeParam3;
      case 4: return variationModeParam4;
      case 5: return variationModeParam5;
      default: throw new IndexOutOfBoundsException("Index of Snapshot.variationModeParam is between 1 and 5");
    }
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
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
