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
package org.sonar.db.component;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.db.component.ComponentValidator.checkComponentKey;
import static org.sonar.db.component.ComponentValidator.checkComponentName;

public class ResourceDto {

  private Long id;
  private String uuid;
  private String projectUuid;
  private String moduleUuid;
  private String moduleUuidPath;
  private String key;
  private String deprecatedKey;
  private String name;
  private String longName;
  private String rootUuid;
  private String path;
  private String scope;
  private String qualifier;
  private boolean enabled = true;
  private String description;
  private String language;
  private String copyComponentUuid;
  private Date createdAt;

  public Long getId() {
    return id;
  }

  public ResourceDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public ResourceDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public ResourceDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getModuleUuid() {
    return moduleUuid;
  }

  public ResourceDto setModuleUuid(String moduleUuid) {
    this.moduleUuid = moduleUuid;
    return this;
  }

  public String getModuleUuidPath() {
    return moduleUuidPath;
  }

  public ResourceDto setModuleUuidPath(String moduleUuidPath) {
    this.moduleUuidPath = moduleUuidPath;
    return this;
  }

  public String getName() {
    return name;
  }

  public ResourceDto setName(String name) {
    this.name = checkComponentName(name);
    return this;
  }

  public String getKey() {
    return key;
  }

  public ResourceDto setKey(String s) {
    this.key = checkComponentKey(s);
    return this;
  }

  public String getDeprecatedKey() {
    return deprecatedKey;
  }

  public ResourceDto setDeprecatedKey(String s) {
    this.deprecatedKey = s;
    return this;
  }

  public String getRootUuid() {
    return rootUuid;
  }

  public ResourceDto setRootUuid(String rootUuid) {
    this.rootUuid = rootUuid;
    return this;
  }

  public String getPath() {
    return path;
  }

  public ResourceDto setPath(String s) {
    this.path = s;
    return this;
  }

  public String getLongName() {
    return longName;
  }

  public ResourceDto setLongName(String longName) {
    this.longName = longName;
    return this;
  }

  public String getScope() {
    return scope;
  }

  public ResourceDto setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public ResourceDto setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public ResourceDto setEnabled(boolean b) {
    this.enabled = b;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public ResourceDto setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public ResourceDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public String getCopyComponentUuid() {
    return copyComponentUuid;
  }

  public ResourceDto setCopyComponentUuid(@Nullable String copyComponentUuid) {
    this.copyComponentUuid = copyComponentUuid;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ResourceDto setCreatedAt(Date date) {
    this.createdAt = date;
    return this;
  }
}
