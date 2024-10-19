/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
  private final String entityUuid;

  private AuthorizationDoc(IndexType indexType, String entityUuid) {
    super(indexType);
    this.entityUuid = entityUuid;
  }

  public static AuthorizationDoc fromDto(IndexType indexType, IndexPermissions dto) {
    AuthorizationDoc res = new AuthorizationDoc(indexType, dto.getEntityUuid());
    if (dto.isAllowAnyone()) {
      return res.setAllowAnyone();
    }
    return res.setRestricted(dto.getGroupUuids(), dto.getUserUuids());
  }

  @Override
  public String getId() {
    return idOf(entityUuid);
  }

  public static String idOf(String entityUuid) {
    requireNonNull(entityUuid, "entityUuid can't be null");
    return ID_PREFIX + entityUuid;
  }

  public static String entityUuidOf(String id) {
    if (id.startsWith(ID_PREFIX)) {
      return id.substring(ID_PREFIX.length());
    }
    return id;
  }

  @Override
  protected Optional<String> getSimpleMainTypeRouting() {
    return Optional.of(entityUuid);
  }

  private AuthorizationDoc setAllowAnyone() {
    setField(FIELD_ALLOW_ANYONE, true);
    return this;
  }

  private AuthorizationDoc setRestricted(List<String> groupUuids, List<String> userUuids) {
    setField(FIELD_ALLOW_ANYONE, false);
    setField(FIELD_GROUP_IDS, groupUuids);
    setField(FIELD_USER_IDS, userUuids);
    return this;
  }
}
