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
package org.sonar.core.measure;

import org.apache.commons.lang.StringUtils;

import java.sql.Timestamp;

public class MeasureFilterRow {
  private final long snapshotId;
  private final long resourceId;
  private final long resourceRootId;
  private String sortText = null;
  private Timestamp sortDate = null;
  private Double sortDouble = null;

  MeasureFilterRow(long snapshotId, long resourceId, long resourceRootId) {
    this.snapshotId = snapshotId;
    this.resourceId = resourceId;
    this.resourceRootId = resourceRootId;
  }

  public long getSnapshotId() {
    return snapshotId;
  }

  public long getResourceId() {
    return resourceId;
  }

  public long getResourceRootId() {
    return resourceRootId;
  }

  public String getSortText() {
    return sortText;
  }

  void setSortText(String s) {
    this.sortText = StringUtils.defaultString(s);
  }

  Timestamp getSortDate() {
    return sortDate;
  }

  void setSortDate(Timestamp sortDate) {
    this.sortDate = sortDate;
  }

  Double getSortDouble() {
    return sortDouble;
  }

  void setSortDouble(Double sortDouble) {
    this.sortDouble = sortDouble;
  }
}
