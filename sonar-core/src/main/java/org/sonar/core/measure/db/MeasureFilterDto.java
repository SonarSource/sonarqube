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
package org.sonar.core.measure.db;

import javax.annotation.Nullable;

import java.util.Date;

/**
 * @since 3.4
 */
public class MeasureFilterDto {
  private Long id;
  private String name;
  private Long userId;
  private Boolean shared;
  private String description;
  private String data;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public MeasureFilterDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public MeasureFilterDto setName(String name) {
    this.name = name;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public MeasureFilterDto setUserId(@Nullable Long userId) {
    this.userId = userId;
    return this;
  }

  public Boolean isShared() {
    return shared;
  }

  public MeasureFilterDto setShared(@Nullable Boolean shared) {
    this.shared = shared;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public MeasureFilterDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getData() {
    return data;
  }

  public MeasureFilterDto setData(String data) {
    this.data = data;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public MeasureFilterDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public MeasureFilterDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
