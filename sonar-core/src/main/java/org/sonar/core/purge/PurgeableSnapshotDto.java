/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.purge;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.Date;

public class PurgeableSnapshotDto implements Comparable<PurgeableSnapshotDto> {
  private Date date;
  private long snapshotId;
  private boolean hasEvents;
  private boolean isLast;

  public Date getDate() {
    return date;//NOSONAR May expose internal representation by returning reference to mutable object
  }

  public long getSnapshotId() {
    return snapshotId;
  }

  public boolean hasEvents() {
    return hasEvents;
  }

  public boolean isLast() {
    return isLast;
  }

  public PurgeableSnapshotDto setDate(Date date) {
    this.date = date;//NOSONAR May expose internal representation by incorporating reference to mutable object
    return this;
  }

  public PurgeableSnapshotDto setSnapshotId(long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  public PurgeableSnapshotDto setHasEvents(boolean b) {
    this.hasEvents = b;
    return this;
  }

  public PurgeableSnapshotDto setLast(boolean last) {
    isLast = last;
    return this;
  }

  public int compareTo(PurgeableSnapshotDto other) {
    return date.compareTo(other.date);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PurgeableSnapshotDto that = (PurgeableSnapshotDto) o;
    return snapshotId == that.snapshotId;
  }

  @Override
  public int hashCode() {
    return (int)snapshotId;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).toString();
  }
}
