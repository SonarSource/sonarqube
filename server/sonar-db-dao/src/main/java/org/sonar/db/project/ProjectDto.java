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
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.entity.EntityDto;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.sonar.db.component.DbTagsReader.readDbTags;

public class ProjectDto extends EntityDto {
  private static final String TAGS_SEPARATOR = ",";
  private String tags;
  private CreationMethod creationMethod;
  private boolean aiCodeAssurance;
  private long createdAt;
  private long updatedAt;
  private String organizationUuid;

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

  public ProjectDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
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

  @Override
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

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public ProjectDto setOrganizationUuid(String organizationUuid) {
    this.organizationUuid = organizationUuid;
    return this;
  }

  public ProjectDto setName(String name) {
    this.name = name;
    return this;
  }

  public ProjectDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public ProjectDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public CreationMethod getCreationMethod() {
    return creationMethod;
  }

  public ProjectDto setCreationMethod(CreationMethod creationMethod) {
    this.creationMethod = creationMethod;
    return this;
  }

  public boolean getAiCodeAssurance() {
    return aiCodeAssurance;
  }

  public ProjectDto setAiCodeAssurance(boolean aiCodeAssurance) {
    this.aiCodeAssurance = aiCodeAssurance;
    return this;
  }
}
