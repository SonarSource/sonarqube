/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.event;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.db.event.EventValidator.checkEventCategory;
import static org.sonar.db.event.EventValidator.checkEventDescription;
import static org.sonar.db.event.EventValidator.checkEventName;

public class EventDto {

  public static final String CATEGORY_VERSION = "Version";
  public static final String CATEGORY_ALERT = "Alert";
  public static final String CATEGORY_PROFILE = "Profile";

  private Long id;
  private String uuid;
  private String analysisUuid;
  private String componentUuid;
  private String name;
  private String description;
  private String category;
  private Long date;
  private Long createdAt;
  private String data;

  public Long getId() {
    return id;
  }

  public EventDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public EventDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getAnalysisUuid() {
    return analysisUuid;
  }

  public EventDto setAnalysisUuid(String analysisUuid) {
    this.analysisUuid = analysisUuid;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public EventDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  /**
   * The name of an event should not be null, but we must accept null values as the DB column is not nullable
   */
  public EventDto setName(@Nullable String name) {
    this.name = checkEventName(name);
    return this;
  }

  @CheckForNull
  public String getCategory() {
    return category;
  }

  /**
   * The category of an event should not be null, but we must accept null values as the DB column is not nullable
   */
  public EventDto setCategory(@Nullable String category) {
    this.category = checkEventCategory(category);
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
    this.description = checkEventDescription(description);
    return this;
  }

}
