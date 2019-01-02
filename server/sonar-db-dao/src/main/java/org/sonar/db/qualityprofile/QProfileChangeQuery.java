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
package org.sonar.db.qualityprofile;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import static java.util.Objects.requireNonNull;

public final class QProfileChangeQuery {

  private final String profileUuid;
  private Long fromIncluded;
  private Long toExcluded;
  private int offset = 0;
  private int limit = 100;

  public QProfileChangeQuery(String profileUuid) {
    this.profileUuid = requireNonNull(profileUuid);
  }

  public String getProfileUuid() {
    return profileUuid;
  }

  @CheckForNull
  public Long getFromIncluded() {
    return fromIncluded;
  }

  public void setFromIncluded(@Nullable Long l) {
    this.fromIncluded = l;
  }

  @CheckForNull
  public Long getToExcluded() {
    return toExcluded;
  }

  public void setToExcluded(@Nullable Long l) {
    this.toExcluded = l;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public void setPage(int page, int pageSize) {
    offset = (page -1) * pageSize;
    limit = pageSize;
  }

  public int getTotal() {
    return offset + limit;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
