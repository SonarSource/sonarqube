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
package org.sonar.server.permission;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.user.GroupDto;

/**
 * Reference to a user group, as used internally by the backend. Contrary to
 * {@link GroupUuid}, it supports reference to virtual groups "anyone". In these
 * cases {@link #getUuid()} returns {@code null}
 *
 * @see GroupUuid
 */
@Immutable
public class GroupUuidOrAnyone {

  private final String uuid;

  private GroupUuidOrAnyone(@Nullable String uuid) {
    this.uuid = uuid;
  }

  public boolean isAnyone() {
    return uuid == null;
  }

  @CheckForNull
  public String getUuid() {
    return uuid;
  }

  public static GroupUuidOrAnyone from(@Nullable GroupDto dto) {
    String groupUuid = Optional.ofNullable(dto).map(GroupDto::getUuid).orElse(null);
    return new GroupUuidOrAnyone(groupUuid);
  }

  public static GroupUuidOrAnyone forAnyone() {
    return new GroupUuidOrAnyone(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupUuidOrAnyone that = (GroupUuidOrAnyone) o;
    return Objects.equals(uuid, that.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
