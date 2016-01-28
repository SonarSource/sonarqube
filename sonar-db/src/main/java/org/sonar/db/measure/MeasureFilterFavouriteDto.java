/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.measure;

import java.util.Date;

public class MeasureFilterFavouriteDto {
  private Long id;
  private Long userId;
  private Long measureFilterId;
  private Date createdAt;

  public Long getId() {
    return id;
  }

  public MeasureFilterFavouriteDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public MeasureFilterFavouriteDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public Long getMeasureFilterId() {
    return measureFilterId;
  }

  public MeasureFilterFavouriteDto setMeasureFilterId(Long measureFilterId) {
    this.measureFilterId = measureFilterId;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public MeasureFilterFavouriteDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
