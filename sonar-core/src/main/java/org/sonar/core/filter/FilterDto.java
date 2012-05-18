/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.filter;

import com.google.common.base.Objects;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @since 3.1
 */
public final class FilterDto {
  private Long id;
  private String name;
  private Long userId;
  private boolean shared;
  private boolean favourites;
  private Long resourceId;
  private String defaultView;
  private Long pageSize;
  private Long periodIndex;

  public Long getId() {
    return id;
  }

  public FilterDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public FilterDto setName(String name) {
    this.name = name;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public FilterDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public boolean isShared() {
    return shared;
  }

  public FilterDto setShared(boolean shared) {
    this.shared = shared;
    return this;
  }

  public boolean isFavourites() {
    return favourites;
  }

  public FilterDto setFavourites(boolean favourites) {
    this.favourites = favourites;
    return this;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public FilterDto setResourceId(Long resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getDefaultView() {
    return defaultView;
  }

  public FilterDto setDefaultView(String defaultView) {
    this.defaultView = defaultView;
    return this;
  }

  public Long getPageSize() {
    return pageSize;
  }

  public FilterDto setPageSize(Long pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public Long getPeriodIndex() {
    return periodIndex;
  }

  public FilterDto setPeriodIndex(Long periodIndex) {
    this.periodIndex = periodIndex;
    return this;
  }


  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FilterDto other = (FilterDto) o;
    return Objects.equal(id, other.id) &&
      Objects.equal(name, other.name) &&
      Objects.equal(userId, other.userId) &&
      Objects.equal(shared, other.shared) &&
      Objects.equal(favourites, other.favourites) &&
      Objects.equal(resourceId, other.resourceId) &&
      Objects.equal(defaultView, other.defaultView) &&
      Objects.equal(pageSize, other.pageSize) &&
      Objects.equal(periodIndex, other.periodIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
