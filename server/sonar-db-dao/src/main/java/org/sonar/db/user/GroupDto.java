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
package org.sonar.db.user;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class GroupDto {

  private Integer id;
  private String name;
  private String description;
  private String organizationUuid;
  private Date createdAt;
  private Date updatedAt;

  public Integer getId() {
    return id;
  }

  public GroupDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public GroupDto setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public GroupDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public GroupDto setOrganizationUuid(String s) {
    this.organizationUuid = s;
    return this;
  }

  public GroupDto setCreatedAt(Date d) {
    this.createdAt = d;
    return this;
  }

  public GroupDto setUpdatedAt(Date d) {
    this.updatedAt = d;
    return this;
  }

  public Date getCreatedAt() {
    return this.createdAt;
  }

  public Date getUpdatedAt() {
    return this.updatedAt;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GroupDto{");
    sb.append("id=").append(id);
    sb.append(", name='").append(name).append('\'');
    sb.append(", description='").append(description).append('\'');
    sb.append(", organizationUuid='").append(organizationUuid).append('\'');
    sb.append(", createdAt=").append(createdAt);
    sb.append(", updatedAt=").append(updatedAt);
    sb.append('}');
    return sb.toString();
  }
}
