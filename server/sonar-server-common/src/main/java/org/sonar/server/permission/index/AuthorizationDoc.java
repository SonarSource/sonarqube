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

import java.util.List;
import java.util.Optional;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.IndexType;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_ALLOW_ANYONE;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_GROUP_IDS;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.FIELD_USER_IDS;

public class AuthorizationDoc extends BaseDoc {
  private static final String ID_PREFIX = "auth_";
  private final String projectUuid;

  private AuthorizationDoc(IndexType indexType, String projectUuid) {
    super(indexType);
    this.projectUuid = projectUuid;
  }

  public static AuthorizationDoc fromDto(IndexType indexType, IndexPermissions dto) {
    AuthorizationDoc res = new AuthorizationDoc(indexType, dto.getProjectUuid());
    if (dto.isAllowAnyone()) {
      return res.setAllowAnyone();
    }
    return res.setRestricted(dto.getGroupIds(), dto.getUserIds());
  }

  @Override
  public String getId() {
    return idOf(projectUuid);
  }

  public static String idOf(String projectUuid) {
    requireNonNull(projectUuid, "projectUuid can't be null");
    return ID_PREFIX + projectUuid;
  }

  public static String projectUuidOf(String id) {
    if (id.startsWith(ID_PREFIX)) {
      return id.substring(ID_PREFIX.length());
    }
    return id;
  }

  @Override
  protected Optional<String> getSimpleMainTypeRouting() {
    return Optional.of(projectUuid);
  }

  private AuthorizationDoc setAllowAnyone() {
    setField(FIELD_ALLOW_ANYONE, true);
    return this;
  }

  private AuthorizationDoc setRestricted(List<Integer> groupIds, List<Integer> userIds) {
    setField(FIELD_ALLOW_ANYONE, false);
    setField(FIELD_GROUP_IDS, groupIds);
    setField(FIELD_USER_IDS, userIds);
    return this;
  }
}
