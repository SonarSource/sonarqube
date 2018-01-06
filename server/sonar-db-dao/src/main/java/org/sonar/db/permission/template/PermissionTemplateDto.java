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
package org.sonar.db.permission.template;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class PermissionTemplateDto {

  private Long id;
  private String name;
  private String organizationUuid;
  private String uuid;
  private String description;
  private String keyPattern;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public PermissionTemplateDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public PermissionTemplateDto setOrganizationUuid(String s) {
    this.organizationUuid = s;
    return this;
  }

  public String getName() {
    return name;
  }

  public PermissionTemplateDto setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @deprecated since 5.2 use {@link #getUuid()}
   */
  @Deprecated
  public String getKee() {
    return uuid;
  }

  /**
   * @deprecated since 5.2 use {@link #setUuid(String)}
   */
  @Deprecated
  public PermissionTemplateDto setKee(String kee) {
    this.uuid = kee;
    return this;
  }

  /**
   * @since 5.2 the kee column is a proper uuid. Before that it was build on the name + timestamp
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * @since 5.2 the kee column is a proper uuid. Before it was build on the name + timestamp
   */
  public PermissionTemplateDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public PermissionTemplateDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public String getKeyPattern() {
    return keyPattern;
  }

  public PermissionTemplateDto setKeyPattern(@Nullable String regexp) {
    this.keyPattern = regexp;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public PermissionTemplateDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public PermissionTemplateDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
