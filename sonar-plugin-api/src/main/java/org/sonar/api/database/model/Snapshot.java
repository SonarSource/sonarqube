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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.database.DatabaseSession;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Date;

import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.longToDate;

/**
 * A class to map a snapshot with its hibernate model
 */
@Entity
@Table(name = "snapshots")
public class Snapshot extends BaseIdentifiable<Snapshot> implements Serializable {

  /**
   * This status is set on the snapshot at the beginning of the batch
   */
  public static final String STATUS_UNPROCESSED = "U";

  /**
   * This status is set on the snapshot at the end of the batch
   */
  public static final String STATUS_PROCESSED = "P";

  @Column(name = "project_id", updatable = true, nullable = true)
  private Integer resourceId;

  @Column(name = "build_date", updatable = true, nullable = true)
  private Long buildDate;

  @Column(name = "created_at", updatable = true, nullable = true)
  private Long createdAt;

  @Column(name = "version", updatable = true, nullable = true, length = 500)
  private String version;

  @Column(name = "islast")
  private Boolean last = Boolean.FALSE;

  @Column(name = "status")
  private String status = STATUS_UNPROCESSED;

  @Column(name = "purge_status", updatable = true, nullable = true)
  private Integer purgeStatus;

  @Column(name = "scope", updatable = true, nullable = true, length = 3)
  private String scope;

  @Column(name = "path", updatable = true, nullable = true, length = 500)
  private String path;

  @Column(name = "depth", updatable = true, nullable = true)
  private Integer depth;

  @Column(name = "qualifier", updatable = true, nullable = true, length = 10)
  private String qualifier;

  @Column(name = "root_snapshot_id", updatable = true, nullable = true)
  private Integer rootId;

  @Column(name = "parent_snapshot_id", updatable = true, nullable = true)
  private Integer parentId;

  @Column(name = "root_project_id", updatable = true, nullable = true)
  private Integer rootProjectId;

  @Column(name = "period1_mode", updatable = true, nullable = true, length = 100)
  private String period1Mode;

  @Column(name = "period2_mode", updatable = true, nullable = true, length = 100)
  private String period2Mode;

  @Column(name = "period3_mode", updatable = true, nullable = true, length = 100)
  private String period3Mode;

  @Column(name = "period4_mode", updatable = true, nullable = true, length = 100)
  private String period4Mode;

  @Column(name = "period5_mode", updatable = true, nullable = true, length = 100)
  private String period5Mode;

  @Column(name = "period1_param", updatable = true, nullable = true, length = 100)
  private String period1Param;

  @Column(name = "period2_param", updatable = true, nullable = true, length = 100)
  private String period2Param;

  @Column(name = "period3_param", updatable = true, nullable = true, length = 100)
  private String period3Param;

  @Column(name = "period4_param", updatable = true, nullable = true, length = 100)
  private String period4Param;

  @Column(name = "period5_param", updatable = true, nullable = true, length = 100)
  private String period5Param;

  @Column(name = "period1_date", updatable = true, nullable = true)
  private Long period1Date;

  @Column(name = "period2_date", updatable = true, nullable = true)
  private Long period2Date;

  @Column(name = "period3_date", updatable = true, nullable = true)
  private Long period3Date;

  @Column(name = "period4_date", updatable = true, nullable = true)
  private Long period4Date;

  @Column(name = "period5_date", updatable = true, nullable = true)
  private Long period5Date;

  public Snapshot() {

  }

  public Snapshot(ResourceModel resource, Snapshot parent) {
    this.resourceId = resource.getId();
    this.qualifier = resource.getQualifier();
    this.scope = resource.getScope();

    if (parent == null) {
      path = "";
      depth = 0;
      this.createdAt = System.currentTimeMillis();

    } else {
      this.parentId = parent.getId();
      this.rootId = (parent.getRootId() == null ? parent.getId() : parent.getRootId());
      this.createdAt = parent.getCreatedAtMs();
      this.depth = parent.getDepth() + 1;
      this.path = new StringBuilder()
        .append(parent.getPath())
        .append(parent.getId())
        .append(".")
        .toString();
    }
    this.rootProjectId = guessRootProjectId(resource, parent);
  }

  public Snapshot(ResourceModel resource, boolean last, String status, Date date) {
    this();
    setResource(resource);
    this.status = status;
    this.last = last;
    this.createdAt = date.getTime();
  }

  private static Integer guessRootProjectId(ResourceModel resource, Snapshot parent) {
    Integer result;
    if (parent == null) {
      result = resource.getId();
    } else {
      result = (parent.getRootProjectId() == null ? parent.getResourceId() : parent.getRootProjectId());
    }
    return result;
  }

  public Snapshot save(DatabaseSession session) {
    return session.save(this);
  }

  /**
   * Insertion date (technical)
   *
   * @since 2.14
   */
  public Date getBuildDate() {
    return longToDate(buildDate);
  }

  /**
   * Insertion date (technical)
   *
   * @since 2.14
   */
  public Snapshot setBuildDate(Date date) {
    this.buildDate = dateToLong(date);
    return this;
  }

  public Long getBuildDateMs() {
    return buildDate;
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

  /**
   * @since 2.14
   */
  public Integer getPurgeStatus() {
    return purgeStatus;
  }

  /**
   * @since 2.14
   */
  public Snapshot setPurgeStatus(Integer i) {
    this.purgeStatus = i;
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

  public String getPeriod1Mode() {
    return period1Mode;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod1Mode(String s) {
    this.period1Mode = s;
    return this;
  }

  public String getPeriod2Mode() {
    return period2Mode;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod2Mode(String s) {
    this.period2Mode = s;
    return this;
  }

  public String getPeriod3Mode() {
    return period3Mode;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod3Mode(String s) {
    this.period3Mode = s;
    return this;
  }

  public String getPeriod4Mode() {
    return period4Mode;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod4Mode(String s) {
    this.period4Mode = s;
    return this;
  }

  public String getPeriod5Mode() {
    return period5Mode;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod5Mode(String s) {
    this.period5Mode = s;
    return this;
  }

  public String getPeriod1Param() {
    return period1Param;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod1Param(String s) {
    this.period1Param = s;
    return this;
  }

  public String getPeriod2Param() {
    return period2Param;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod2Param(String s) {
    this.period2Param = s;
    return this;
  }

  public String getPeriod3Param() {
    return period3Param;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod3Param(String s) {
    this.period3Param = s;
    return this;
  }

  public String getPeriod4Param() {
    return period4Param;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod4Param(String s) {
    this.period4Param = s;
    return this;
  }

  public String getPeriod5Param() {
    return period5Param;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod5Param(String s) {
    this.period5Param = s;
    return this;
  }

  public Date getPeriod1Date() {
    return longToDate(period1Date);
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod1Date(Date period1Date) {
    this.period1Date = dateToLong(period1Date);
    return this;
  }

  public Long getPeriod1DateMs() {
    return period1Date;
  }

  public Snapshot setPeriod1DateMs(Long period1Date) {
    this.period1Date = period1Date;
    return this;
  }

  public Date getPeriod2Date() {
    return longToDate(period2Date);
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod2Date(Date period2Date) {
    this.period2Date = dateToLong(period2Date);
    return this;
  }

  public Long getPeriod2DateMs() {
    return period2Date;
  }

  public Snapshot setPeriod2DateMs(Long period2Date) {
    this.period2Date = period2Date;
    return this;
  }

  public Date getPeriod3Date() {
    return longToDate(period3Date);
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod3Date(Date period3Date) {
    this.period3Date = dateToLong(period3Date);
    return this;
  }

  public Long getPeriod3DateMs() {
    return period3Date;
  }

  public Snapshot setPeriod3DateMs(Long period3Date) {
    this.period3Date = period3Date;
    return this;
  }

  public Date getPeriod4Date() {
    return longToDate(period4Date);
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod4Date(Date period4Date) {
    this.period4Date = dateToLong(period4Date);
    return this;
  }

  public Long getPeriod4DateMs() {
    return period4Date;
  }

  public Snapshot setPeriod4DateMs(Long period4Date) {
    this.period4Date = period4Date;
    return this;
  }

  public Date getPeriod5Date() {
    return longToDate(period5Date);
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriod5Date(Date period5Date) {
    this.period5Date = dateToLong(period5Date);
    return this;
  }

  public Long getPeriod5DateMs() {
    return period5Date;
  }

  public Snapshot setPeriod5DateMs(Long period5Date) {
    this.period5Date = period5Date;
    return this;
  }

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriodMode(int periodIndex, String s) {
    switch (periodIndex) {
      case 1:
        period1Mode = s;
        break;
      case 2:
        period2Mode = s;
        break;
      case 3:
        period3Mode = s;
        break;
      case 4:
        period4Mode = s;
        break;
      case 5:
        period5Mode = s;
        break;
      default:
        throw newPeriodIndexOutOfBoundsException("periodMode");
    }
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

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriodModeParameter(int periodIndex, String s) {
    switch (periodIndex) {
      case 1:
        period1Param = s;
        break;
      case 2:
        period2Param = s;
        break;
      case 3:
        period3Param = s;
        break;
      case 4:
        period4Param = s;
        break;
      case 5:
        period5Param = s;
        break;
      default:
        throw newPeriodIndexOutOfBoundsException("periodParameter");
    }
    return this;
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

  /**
   * For internal use.
   *
   * @since 2.5
   */
  public Snapshot setPeriodDate(int periodIndex, Date date) {
    Long time = dateToLong(date);
    switch (periodIndex) {
      case 1:
        period1Date = time;
        break;
      case 2:
        period2Date = time;
        break;
      case 3:
        period3Date = time;
        break;
      case 4:
        period4Date = time;
        break;
      case 5:
        period5Date = time;
        break;
      default:
        throw newPeriodIndexOutOfBoundsException("periodDate");
    }
    return this;
  }

  public Snapshot setPeriodDateMs(int periodIndex, Long date) {
    switch (periodIndex) {
      case 1:
        period1Date = date;
        break;
      case 2:
        period2Date = date;
        break;
      case 3:
        period3Date = date;
        break;
      case 4:
        period4Date = date;
        break;
      case 5:
        period5Date = date;
        break;
      default:
        throw newPeriodIndexOutOfBoundsException("periodDate");
    }
    return this;
  }

  public Date getPeriodDate(int periodIndex) {
    switch (periodIndex) {
      case 1:
        return longToDate(period1Date);
      case 2:
        return longToDate(period2Date);
      case 3:
        return longToDate(period3Date);
      case 4:
        return longToDate(period4Date);
      case 5:
        return longToDate(period5Date);
      default:
        throw newPeriodIndexOutOfBoundsException("periodDate");
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

  private IndexOutOfBoundsException newPeriodIndexOutOfBoundsException(String field) {
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
