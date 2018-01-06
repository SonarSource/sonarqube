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
import org.sonar.api.security.DefaultGroups;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Reference to a user group <b>as declared by web service requests</b>. It is one, and only one,
 * of these two options:
 * <ul>
 *   <li>group id, for instance 1234</li>
 *   <li>group name and optional organization key</li>
 * </ul>
 *
 * The reference is then converted to a {@link GroupId} or {@link GroupIdOrAnyone}.
 */
@Immutable
public class GroupWsRef {

  private static final int NULL_ID = -1;

  private final int id;
  private final String organizationKey;
  private final String name;

  private GroupWsRef(int id, @Nullable String organizationKey, @Nullable String name) {
    this.id = id;
    this.organizationKey = organizationKey;
    this.name = name;
  }

  /**
   * @return {@code true} if id is defined and {@link #getId()} can be called. If {@code false}, then
   *   the couple {organizationKey, name} is defined and the methods {@link #getOrganizationKey()}/{@link #getName()}
   *   can be called.
   */
  public boolean hasId() {
    return id != NULL_ID;
  }

  /**
   * @return the group id
   * @throws IllegalStateException if {@link #getId()} is {@code false}
   */
  public int getId() {
    checkState(hasId(), "Id is not present. Please see hasId().");
    return id;
  }

  /**
   * @return the organization key
   * @throws IllegalStateException if {@link #getId()} is {@code true}
   */
  @CheckForNull
  public String getOrganizationKey() {
    checkState(!hasId(), "Organization is not present. Please see hasId().");
    return organizationKey;
  }

  /**
   * @return the non-null group name. Can be anyone.
   * @throws IllegalStateException if {@link #getId()} is {@code true}
   */
  public String getName() {
    checkState(!hasId(), "Name is not present. Please see hasId().");
    return name;
  }

  /**
   * Creates a reference to a group by its id. Virtual groups "Anyone" can't be returned
   * as they can't be referenced by an id.
   */
  static GroupWsRef fromId(int id) {
    checkArgument(id > -1, "Group id must be positive: %s", id);
    return new GroupWsRef(id, null, null);
  }

  /**
   * Creates a reference to a group by its organization and name. Virtual groups "Anyone" are
   * supported.
   *
   * @param organizationKey key of organization. If {@code null}, then default organization will be used.
   * @param name non-null name. Can refer to anyone group (case-insensitive {@code "anyone"}).
   */
  static GroupWsRef fromName(@Nullable String organizationKey, String name) {
    return new GroupWsRef(NULL_ID, organizationKey, requireNonNull(name));
  }

  public static GroupWsRef create(@Nullable Integer id, @Nullable String organizationKey, @Nullable String name) {
    if (id != null) {
      checkRequest(organizationKey == null && name == null, "Either group id or couple organization/group name must be set");
      return fromId(id);
    }

    checkRequest(name != null, "Group name or group id must be provided");
    return fromName(organizationKey, name);
  }

  public boolean isAnyone() {
    return !hasId() && DefaultGroups.isAnyone(name);
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
    if (id != that.id) {
      return false;
    }
    if (organizationKey != null ? !organizationKey.equals(that.organizationKey) : (that.organizationKey != null)) {
      return false;
    }
    return name != null ? name.equals(that.name) : (that.name == null);
  }

  @Override
  public int hashCode() {
    int result = id;
    result = 31 * result + (organizationKey != null ? organizationKey.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GroupWsRef{");
    sb.append("id=").append(id);
    sb.append(", organizationKey='").append(organizationKey).append('\'');
    sb.append(", name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
