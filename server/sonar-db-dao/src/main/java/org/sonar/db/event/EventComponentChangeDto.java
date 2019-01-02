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
package org.sonar.db.event;

import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;

public class EventComponentChangeDto {
  private String uuid;
  private String eventUuid;
  private ChangeCategory category;
  private String componentUuid;
  private String componentKey;
  private String componentName;
  @Nullable
  private String componentBranchKey;
  /**read-only*/
  private long createdAt;

  public enum ChangeCategory {
    FAILED_QUALITY_GATE("FAILED_QG"), ADDED("ADDED"), REMOVED("REMOVED");

    private final String dbValue;

    ChangeCategory(String dbValue) {
      this.dbValue = dbValue;
    }

    public static Optional<ChangeCategory> fromDbValue(String dbValue) {
      return Arrays.stream(values())
        .filter(t -> t.dbValue.equals(dbValue))
        .findAny();
    }
  }

  public String getUuid() {
    return uuid;
  }

  public EventComponentChangeDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getEventUuid() {
    return eventUuid;
  }

  public EventComponentChangeDto setEventUuid(String eventUuid) {
    this.eventUuid = eventUuid;
    return this;
  }

  public ChangeCategory getCategory() {
    return category;
  }

  public EventComponentChangeDto setCategory(ChangeCategory category) {
    this.category = category;
    return this;
  }

  /**
   * Used by MyBatis through reflection.
   */
  private String getChangeCategory() {
    return category == null ? null : category.dbValue;
  }

  /**
   * Used by MyBatis through reflection.
   *
   * @throws IllegalArgumentException if not a support change category DB value
   */
  private EventComponentChangeDto setChangeCategory(String changeCategory) {
    this.category = ChangeCategory.fromDbValue(changeCategory)
      .orElseThrow(() -> new IllegalArgumentException("Unsupported changeCategory DB value: " + changeCategory));
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public EventComponentChangeDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

  public EventComponentChangeDto setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public String getComponentName() {
    return componentName;
  }

  public EventComponentChangeDto setComponentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  @Nullable
  public String getComponentBranchKey() {
    return componentBranchKey;
  }

  public EventComponentChangeDto setComponentBranchKey(@Nullable String componentBranchKey) {
    this.componentBranchKey = componentBranchKey;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  /**
   * Used by MyBatis through reflection.
   */
  private void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}
