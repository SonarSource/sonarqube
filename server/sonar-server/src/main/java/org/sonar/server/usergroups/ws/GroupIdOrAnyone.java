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
package org.sonar.server.usergroups.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.user.GroupDto;

import static java.util.Objects.requireNonNull;

/**
 * Reference to a user group, as used internally by the backend. Contrary to
 * {@link GroupId}, it supports reference to virtual groups "anyone". In these
 * cases {@link #getId()} returns {@code null}
 *
 * @see GroupWsRef
 * @see GroupId
 */
@Immutable
public class GroupIdOrAnyone {

  private final Integer id;
  private final String organizationUuid;

  private GroupIdOrAnyone(String organizationUuid, @Nullable Integer id) {
    this.id = id;
    this.organizationUuid = requireNonNull(organizationUuid);
  }

  public boolean isAnyone() {
    return id == null;
  }

  @CheckForNull
  public Integer getId() {
    return id;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public static GroupIdOrAnyone from(GroupDto dto) {
    return new GroupIdOrAnyone(dto.getOrganizationUuid(), dto.getId());
  }

  public static GroupIdOrAnyone forAnyone(String organizationUuid) {
    return new GroupIdOrAnyone(organizationUuid, null);
  }
}
