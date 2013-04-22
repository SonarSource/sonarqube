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
package org.sonar.api.database.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.database.DatabaseProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "snapshot_sources")
public class SnapshotSource extends BaseIdentifiable {

  @Column(name = "snapshot_id")
  private Integer snapshotId;

  @Lob
  @Column(name = "data", updatable = true, nullable = true, length = DatabaseProperties.MAX_TEXT_SIZE)
  private String data;

  public SnapshotSource() {
  }

  public SnapshotSource(Snapshot snapshot, String source) {
    this.snapshotId = snapshot.getId();
    this.data = source;
  }

  public SnapshotSource(Integer snapshotId, String source) {
    this.snapshotId = snapshotId;
    this.data = source;
  }

  public void setSnapshot(Snapshot snapshot) {
    this.snapshotId = snapshot.getId();
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SnapshotSource)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    SnapshotSource other = (SnapshotSource) obj;
    return snapshotId.equals(other.snapshotId);
  }

  @Override
  public int hashCode() {
    return snapshotId.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("snapshot_id", snapshotId)
        .append("data", StringUtils.abbreviate(data, 1000))
        .toString();
  }
}
