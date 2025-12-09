/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
  private final String entityUuid;
  private final String qualifier;
  private final List<String> userUuids = new ArrayList<>();
  private final List<String> groupUuids = new ArrayList<>();
  private boolean allowAnyone = false;

  public IndexPermissions(String entityUuid, String qualifier) {
    this.entityUuid = entityUuid;
    this.qualifier = qualifier;
  }

  public String getEntityUuid() {
    return entityUuid;
  }

  public String getQualifier() {
    return qualifier;
  }

  public List<String> getUserUuids() {
    return userUuids;
  }

  public IndexPermissions addUserUuid(String l) {
    userUuids.add(l);
    return this;
  }

  public IndexPermissions addGroupUuid(String uuid) {
    groupUuids.add(uuid);
    return this;
  }

  public List<String> getGroupUuids() {
    return groupUuids;
  }

  public IndexPermissions allowAnyone() {
    this.allowAnyone = true;
    return this;
  }

  public boolean isAllowAnyone() {
    return allowAnyone;
  }
}
