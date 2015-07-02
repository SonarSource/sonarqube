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
package org.sonar.api.database.model;

import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.BaseIdentifiable;

import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.longToDate;

public class Snapshot extends BaseIdentifiable<Snapshot> implements Serializable {

  /**
   * This status is set on the snapshot at the beginning of the batch
   */
  public static final String STATUS_UNPROCESSED = "U";

  /**
   * This status is set on the snapshot at the end of the batch
   */
  public static final String STATUS_PROCESSED = "P";

  private Integer resourceId;
  private Long buildDate;
  private Long createdAt;
  private String version;
  private Boolean last = Boolean.FALSE;
  private String status = STATUS_UNPROCESSED;
  private Integer purgeStatus;
  private String scope;
  private String path;
  private Integer depth;
  private String qualifier;
  private Integer rootId;
  private Integer parentId;
  private Integer rootProjectId;
  private String period1Mode;
  private String period2Mode;
  private String period3Mode;
  private String period4Mode;
  private String period5Mode;
  private String period1Param;
  private String period2Param;
  private String period3Param;
  private String period4Param;
  private String period5Param;
  private Long period1Date;
  private Long period2Date;
  private Long period3Date;
  private Long period4Date;
  private Long period5Date;

  public Snapshot() {

  }

  public Snapshot setBuildDateMs(Long d) {
    this.buildDate = d;
    return this;
  }

  public Date getCreatedAt() {
    return longToDate(createdAt);
  }

  public Snapshot setCreatedAt(Date createdAt) {
    this.createdAt = dateToLong(createdAt);
    return this;
  }

  public Long getCreatedAtMs() {
    return createdAt;
  }

  public Snapshot setCreatedAtMs(Long createdAt) {
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

  public Integer getRootProjectId() {
    return rootProjectId;
  }

  public Snapshot setRootProjectId(Integer rootProjectId) {
    this.rootProjectId = rootProjectId;
    return this;
  }

  public String getPeriodMode(int index) {
    switch (index) {
      case 1:
        return period1Mode;
      case 2:
        return period2Mode;
      case 3:
        return period3Mode;
      case 4:
        return period4Mode;
      case 5:
        return period5Mode;
      default:
        throw newPeriodIndexOutOfBoundsException("periodMode");
    }
  }

  public String getPeriodModeParameter(int periodIndex) {
    switch (periodIndex) {
      case 1:
        return period1Param;
      case 2:
        return period2Param;
      case 3:
        return period3Param;
      case 4:
        return period4Param;
      case 5:
        return period5Param;
      default:
        throw newPeriodIndexOutOfBoundsException("periodParameter");
    }
  }

  public Long getPeriodDateMs(int periodIndex) {
    switch (periodIndex) {
      case 1:
        return period1Date;
      case 2:
        return period2Date;
      case 3:
        return period3Date;
      case 4:
        return period4Date;
      case 5:
        return period5Date;
      default:
        throw newPeriodIndexOutOfBoundsException("periodDate");
    }
  }

  private static IndexOutOfBoundsException newPeriodIndexOutOfBoundsException(String field) {
    return new IndexOutOfBoundsException(String.format("Index of Snapshot.%s is between 1 and 5", field));
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
      .append(createdAt, other.getCreatedAtMs())
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
