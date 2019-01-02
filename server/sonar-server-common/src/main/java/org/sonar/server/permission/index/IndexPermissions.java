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
package org.sonar.server.permission.index;

import java.util.ArrayList;
import java.util.List;

public final class IndexPermissions {
  private final String projectUuid;
  private final String qualifier;
  private final List<Integer> userIds = new ArrayList<>();
  private final List<Integer> groupIds = new ArrayList<>();
  private boolean allowAnyone = false;

  public IndexPermissions(String projectUuid, String qualifier) {
    this.projectUuid = projectUuid;
    this.qualifier = qualifier;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public String getQualifier() {
    return qualifier;
  }

  public List<Integer> getUserIds() {
    return userIds;
  }

  public IndexPermissions addUserId(int l) {
    userIds.add(l);
    return this;
  }

  public IndexPermissions addGroupId(int id) {
    groupIds.add(id);
    return this;
  }

  public List<Integer> getGroupIds() {
    return groupIds;
  }

  public IndexPermissions allowAnyone() {
    this.allowAnyone = true;
    return this;
  }

  public boolean isAllowAnyone() {
    return allowAnyone;
  }
}
