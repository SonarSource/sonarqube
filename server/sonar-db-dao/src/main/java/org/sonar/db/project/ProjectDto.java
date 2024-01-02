/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.project;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.db.component.DbTagsReader.readDbTags;

public class ProjectDto {
  private static final String TAGS_SEPARATOR = ",";
  private String uuid;
  private String kee;
  private String qualifier;
  private String name;
  private String description;
  private boolean isPrivate = false;
  private String tags;
  private long createdAt;
  private long updatedAt;

  public ProjectDto() {
    // nothing to do here
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public ProjectDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public ProjectDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public ProjectDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * This is the getter used by MyBatis mapper.
   */
  public String getKee() {
    return kee;
  }

  public String getKey() {
    return getKee();
  }

  /**
   * This is the setter used by MyBatis mapper.
   */
  public ProjectDto setKee(String kee) {
    this.kee = kee;
    return this;
  }

  public ProjectDto setKey(String key) {
    return setKee(key);
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public ProjectDto setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
    return this;
  }

  public List<String> getTags() {
    return readDbTags(tags);
  }

  public ProjectDto setTags(List<String> tags) {
    setTagsString(tags.stream()
      .filter(t -> !t.isEmpty())
      .collect(Collectors.joining(TAGS_SEPARATOR)));
    return this;
  }

  /**
   * Used by MyBatis
   */
  @CheckForNull
  public String getTagsString() {
    return tags;
  }

  public ProjectDto setTagsString(@Nullable String tags) {
    this.tags = trimToNull(tags);
    return this;
  }

  public String getName() {
    return name;
  }

  public ProjectDto setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public ProjectDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public ProjectDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDto that = (ProjectDto) o;
    return Objects.equals(uuid, that.uuid);
  }

  @Override
  public int hashCode() {
    return uuid != null ? uuid.hashCode() : 0;
  }

}
