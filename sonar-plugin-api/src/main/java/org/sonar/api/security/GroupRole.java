/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.security;

import org.sonar.api.database.BaseIdentifiable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @since 1.12
 */
@Entity
@Table(name = "group_roles")
public class GroupRole extends BaseIdentifiable {

  public static final Integer ANYONE_GROUP_ID = null;

  @Column(name = "group_id")
  private Integer groupId;

  @Column(name = "role")
  private String role;

  @Column(name = "resource_id")
  private Integer resourceId;

  public static GroupRole buildGlobalRole(Integer groupId, String role) {
    return new GroupRole().setGroupId(groupId).setRole(role);
  }

  public static GroupRole buildResourceRole(Integer groupId, String role, Integer resourceId) {
    return new GroupRole().setGroupId(groupId).setRole(role).setResourceId(resourceId);
  }

  public Integer getGroupId() {
    return groupId;
  }

  public GroupRole setGroupId(Integer groupId) {
    this.groupId = groupId;
    return this;
  }

  public String getRole() {
    return role;
  }

  public GroupRole setRole(String role) {
    this.role = role;
    return this;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public GroupRole setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public boolean isAnyone() {
    return groupId==ANYONE_GROUP_ID;
  }
}
