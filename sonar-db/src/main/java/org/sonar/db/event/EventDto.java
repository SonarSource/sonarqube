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
package org.sonar.db.event;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class EventDto {

  public static final String CATEGORY_VERSION = "Version";
  public static final String CATEGORY_ALERT = "Alert";
  public static final String CATEGORY_PROFILE = "Profile";

  private Long id;

  private String name;

  private String description;

  private String category;

  private Long date;

  private Long createdAt;

  private String data;

  private Long snapshotId;

  private String componentUuid;

  public Long getId() {
    return id;
  }

  public EventDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getCategory() {
    return category;
  }

  public EventDto setCategory(String category) {
    this.category = category;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public EventDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public EventDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @CheckForNull
  public String getData() {
    return data;
  }

  public EventDto setData(@Nullable String data) {
    this.data = data;
    return this;
  }

  public Long getDate() {
    return date;
  }

  public EventDto setDate(Long date) {
    this.date = date;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public EventDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getName() {
    return name;
  }

  public EventDto setName(String name) {
    this.name = name;
    return this;
  }

  public Long getSnapshotId() {
    return snapshotId;
  }

  public EventDto setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }
}
