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
package org.sonar.db.component;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Scopes;

public class ComponentDto implements Component {

  public static final String MODULE_UUID_PATH_SEP = ".";

  private Long id;
  private String uuid;
  private String kee;
  private String scope;
  private String qualifier;

  private String projectUuid;
  private String moduleUuid;
  private String moduleUuidPath;
  private Long parentProjectId;
  private Long copyResourceId;
  private Long developerId;

  private String path;
  private String deprecatedKey;
  private String name;
  private String longName;
  private String language;
  private String description;
  private boolean enabled = true;

  private Date createdAt;
  private Long authorizationUpdatedAt;

  public Long getId() {
    return id;
  }

  public ComponentDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String uuid() {
    return uuid;
  }

  public ComponentDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  @Override
  public String key() {
    return kee;
  }

  public String scope() {
    return scope;
  }

  public ComponentDto setScope(String scope) {
    this.scope = scope;
    return this;
  }

  @Override
  public String qualifier() {
    return qualifier;
  }

  public ComponentDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  @CheckForNull
  public String deprecatedKey() {
    return deprecatedKey;
  }

  public ComponentDto setDeprecatedKey(@Nullable String deprecatedKey) {
    this.deprecatedKey = deprecatedKey;
    return this;
  }

  /**
   * Return the root project uuid. On a root project, return itself
   */
  public String projectUuid() {
    return projectUuid;
  }

  public ComponentDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  /**
   * Return the direct module of a component. Will be null on projects
   */
  @CheckForNull
  public String moduleUuid() {
    return moduleUuid;
  }

  public ComponentDto setModuleUuid(@Nullable String moduleUuid) {
    this.moduleUuid = moduleUuid;
    return this;
  }

  /**
   * Return the path from the project to the last modules
   */
  public String moduleUuidPath() {
    return moduleUuidPath;
  }

  public ComponentDto setModuleUuidPath(String moduleUuidPath) {
    this.moduleUuidPath = moduleUuidPath;
    return this;
  }

  @CheckForNull
  @Override
  public String path() {
    return path;
  }

  public ComponentDto setPath(@Nullable String path) {
    this.path = path;
    return this;
  }

  @Override
  public String name() {
    return name;
  }

  public ComponentDto setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String longName() {
    return longName;
  }

  public ComponentDto setLongName(String longName) {
    this.longName = longName;
    return this;
  }

  @CheckForNull
  public String language() {
    return language;
  }

  public ComponentDto setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public String description() {
    return description;
  }

  public ComponentDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public Long parentProjectId() {
    return parentProjectId;
  }

  public ComponentDto setParentProjectId(@Nullable Long parentProjectId) {
    this.parentProjectId = parentProjectId;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public ComponentDto setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public Long getCopyResourceId() {
    return copyResourceId;
  }

  public ComponentDto setCopyResourceId(Long copyResourceId) {
    this.copyResourceId = copyResourceId;
    return this;
  }

  public Long getDeveloperId() {
    return developerId;
  }

  public ComponentDto setDeveloperId(Long developerId) {
    this.developerId = developerId;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ComponentDto setCreatedAt(Date datetime) {
    this.createdAt = datetime;
    return this;
  }

  /**
   * Only available on projects
   */
  @CheckForNull
  public Long getAuthorizationUpdatedAt() {
    return authorizationUpdatedAt;
  }

  public ComponentDto setAuthorizationUpdatedAt(@Nullable Long authorizationUpdatedAt) {
    this.authorizationUpdatedAt = authorizationUpdatedAt;
    return this;
  }

  public String getKey() {
    return key();
  }

  public ComponentDto setKey(String key) {
    this.kee = key;
    return this;
  }

  public boolean isRootProject() {
    return moduleUuid == null && Scopes.PROJECT.equals(scope);
  }

  // FIXME equals/hashCode mean nothing on DTOs, especially when on id
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentDto that = (ComponentDto) o;
    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    return true;
  }

  // FIXME equals/hashCode mean nothing on DTOs, especially when on id
  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", id)
      .append("uuid", uuid)
      .append("kee", kee)
      .append("scope", scope)
      .append("qualifier", qualifier)
      .append("projectUuid", projectUuid)
      .append("moduleUuid", moduleUuid)
      .append("moduleUuidPath", moduleUuidPath)
      .append("parentProjectId", parentProjectId)
      .append("copyResourceId", copyResourceId)
      .append("developerId", developerId)
      .append("path", path)
      .append("deprecatedKey", deprecatedKey)
      .append("name", name)
      .append("longName", longName)
      .append("language", language)
      .append("enabled", enabled)
      .append("authorizationUpdatedAt", authorizationUpdatedAt)
      .toString();
  }

}
