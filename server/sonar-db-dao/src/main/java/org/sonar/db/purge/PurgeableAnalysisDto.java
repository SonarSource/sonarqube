/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.purge;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Represents an analysis, aka. root snapshot, aka. snapshot of a project or portfolio
 */
public class PurgeableAnalysisDto implements Comparable<PurgeableAnalysisDto> {
  private Date date;
  private long analysisId;
  private String analysisUuid;
  private String version;
  private boolean hasEvents;
  private boolean isLast;

  public Date getDate() {
    return date;
  }

  public PurgeableAnalysisDto setDate(Long aLong) {
    this.date = new Date(aLong);
    return this;
  }

  public long getAnalysisId() {
    return analysisId;
  }

  public PurgeableAnalysisDto setAnalysisId(long analysisId) {
    this.analysisId = analysisId;
    return this;
  }

  public String getAnalysisUuid() {
    return analysisUuid;
  }

  public PurgeableAnalysisDto setAnalysisUuid(String analysisUuid) {
    this.analysisUuid = analysisUuid;
    return this;
  }

  public boolean hasEvents() {
    return hasEvents;
  }

  public PurgeableAnalysisDto setHasEvents(boolean b) {
    this.hasEvents = b;
    return this;
  }

  public boolean isLast() {
    return isLast;
  }

  public PurgeableAnalysisDto setLast(boolean last) {
    isLast = last;
    return this;
  }

  @CheckForNull
  public String getVersion() {
    return version;
  }

  public PurgeableAnalysisDto setVersion(@Nullable String version) {
    this.version = version;
    return this;
  }

  @Override
  public int compareTo(PurgeableAnalysisDto other) {
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
    PurgeableAnalysisDto that = (PurgeableAnalysisDto) o;
    return analysisUuid.equals(that.analysisUuid);
  }

  @Override
  public int hashCode() {
    return analysisUuid.hashCode();
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).toString();
  }
}
