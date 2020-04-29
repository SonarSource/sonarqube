/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.server.permission.GroupUuid;
import org.sonar.server.permission.GroupUuidOrAnyone;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

/**
 * Reference to a user group <b>as declared by web service requests</b>. It is one, and only one,
 * of these two options:
 * <ul>
 *   <li>group uuid, for instance 1234</li>
 *   <li>group name and optional organization key</li>
 * </ul>
 *
 * The reference is then converted to a {@link GroupUuid} or {@link GroupUuidOrAnyone}.
 */
@Immutable
public class GroupWsRef {

  private final String uuid;
  private final String organizationKey;
  private final String name;

  private GroupWsRef(String uuid, @Nullable String organizationKey, @Nullable String name) {
    this.uuid = uuid;
    this.organizationKey = organizationKey;
    this.name = name;
  }

  /**
   * @return {@code true} if uuid is defined and {@link #getUuid()} can be called. If {@code false}, then
   *   the couple {organizationKey, name} is defined and the methods {@link #getOrganizationKey()}/{@link #getName()}
   *   can be called.
   */
  public boolean hasUuid() {
    return uuid != null;
  }

  /**
   * @return the group uuid
   * @throws IllegalStateException if {@link #getUuid()} is {@code false}
   */
  public String getUuid() {
    checkState(hasUuid(), "Id is not present. Please see hasUuid().");
    return uuid;
  }

  /**
   * @return the organization key
   * @throws IllegalStateException if {@link #getUuid()} is {@code true}
   */
  @CheckForNull
  public String getOrganizationKey() {
    checkState(!hasUuid(), "Organization is not present. Please see hasId().");
    return organizationKey;
  }

  /**
   * @return the non-null group name. Can be anyone.
   * @throws IllegalStateException if {@link #getUuid()} is {@code true}
   */
  public String getName() {
    checkState(!hasUuid(), "Name is not present. Please see hasId().");
    return name;
  }

  /**
   * Creates a reference to a group by its uuid. Virtual groups "Anyone" can't be returned
   * as they can't be referenced by an uuid.
   */
  static GroupWsRef fromUuid(String uuid) {
    return new GroupWsRef(uuid, null, null);
  }

  /**
   * Creates a reference to a group by its organization and name. Virtual groups "Anyone" are
   * supported.
   *
   * @param organizationKey key of organization. If {@code null}, then default organization will be used.
   * @param name non-null name. Can refer to anyone group (case-insensitive {@code "anyone"}).
   */
  static GroupWsRef fromName(@Nullable String organizationKey, String name) {
    return new GroupWsRef(null, organizationKey, requireNonNull(name));
  }

  public static GroupWsRef create(@Nullable String uuid, @Nullable String organizationKey, @Nullable String name) {
    if (uuid != null) {
      checkRequest(organizationKey == null && name == null, "Either group id or couple organization/group name must be set");
      return fromUuid(uuid);
    }

    checkRequest(name != null, "Group name or group id must be provided");
    return fromName(organizationKey, name);
  }

  public boolean isAnyone() {
    return !hasUuid() && DefaultGroups.isAnyone(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupWsRef that = (GroupWsRef) o;
    return Objects.equals(uuid, that.uuid) && Objects.equals(organizationKey, that.organizationKey) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, organizationKey, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GroupWsRef{");
    sb.append("uuid=").append(uuid);
    sb.append(", organizationKey='").append(organizationKey).append('\'');
    sb.append(", name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
