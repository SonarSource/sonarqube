/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * No streaming because of union of joins -> no need to use ResultSetIterator
 */
public class PermissionIndexerDao {

  private enum RowKind {
    USER, GROUP, ANYONE, NONE
  }

  private static final String SQL_TEMPLATE = """
    with entity as ((select prj.uuid      as uuid,
                            prj.private   as isPrivate,
                            prj.qualifier as qualifier
                     from projects prj)
                    union
                    (select p.uuid    as uuid,
                            p.private as isPrivate,
                            'VW'      as qualifier
                     from portfolios p
                     where p.parent_uuid is null))
    SELECT entity_authorization.kind       as kind,
           entity_authorization.entity     as entity,
           entity_authorization.user_uuid  as user_uuid,
           entity_authorization.group_uuid as group_uuid,
           entity_authorization.qualifier  as qualifier
    FROM (SELECT '%s'                 as kind,
                 e.uuid               AS entity,
                 e.qualifier          AS qualifier,
                 user_roles.user_uuid AS user_uuid,
                 NULL                 AS group_uuid
          FROM entity e
                   INNER JOIN user_roles ON user_roles.entity_uuid = e.uuid AND user_roles.role = 'user'
          WHERE (1 = 1)
                {entitiesCondition}
          UNION
          SELECT '%s' as kind, e.uuid AS entity, e.qualifier AS qualifier, NULL AS user_uuid, groups.uuid AS group_uuid
          FROM entity e
                   INNER JOIN group_roles
                              ON group_roles.entity_uuid = e.uuid AND group_roles.role = 'user'
                   INNER JOIN groups ON groups.uuid = group_roles.group_uuid
          WHERE group_uuid IS NOT NULL
                {entitiesCondition}
          UNION
          SELECT '%s' as kind, e.uuid AS entity, e.qualifier AS qualifier, NULL AS user_uuid, NULL AS group_uuid
          FROM entity e
          WHERE e.isPrivate = ?
                {entitiesCondition}
          UNION
          SELECT '%s' as kind, e.uuid AS entity, e.qualifier AS qualifier, NULL AS user_uuid, NULL AS group_uuid
          FROM entity e
          WHERE e.isPrivate = ?
             {entitiesCondition}
         ) entity_authorization""".formatted(RowKind.USER, RowKind.GROUP, RowKind.ANYONE, RowKind.NONE);

  List<IndexPermissions> selectAll(DbClient dbClient, DbSession session) {
    return doSelectByEntities(dbClient, session, Collections.emptyList());
  }

  public List<IndexPermissions> selectByUuids(DbClient dbClient, DbSession session, Collection<String> entitiesUuid) {
    // we use a smaller partitionSize because the SQL_TEMPLATE contain 4x the list of entity uuid.
    // the MsSQL jdbc driver accept a maximum of 2100 prepareStatement parameter. To stay under the limit,
    // we go with batch of 1000/2=500 entities uuids, to stay under the limit (4x500 < 2100)
    return executeLargeInputs(entitiesUuid, entity -> doSelectByEntities(dbClient, session, entity), i -> i / 2);
  }

  private static List<IndexPermissions> doSelectByEntities(DbClient dbClient, DbSession session, List<String> entitiesUuids) {
    try {
      Map<String, IndexPermissions> dtosByEntityUuid = new HashMap<>();
      try (PreparedStatement stmt = createStatement(dbClient, session, entitiesUuids);
        ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          processRow(rs, dtosByEntityUuid);
        }
        return ImmutableList.copyOf(dtosByEntityUuid.values());
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to select authorizations", e);
    }
  }

  private static PreparedStatement createStatement(DbClient dbClient, DbSession session, List<String> entityUuids) throws SQLException {
    String sql;
    if (entityUuids.isEmpty()) {
      sql = CS.replace(SQL_TEMPLATE, "{entitiesCondition}", "");
    } else {
      sql = CS.replace(SQL_TEMPLATE, "{entitiesCondition}", " AND e.uuid in (" + repeat("?", ", ", entityUuids.size()) + ")");
    }
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
    int index = 1;
    // query for RowKind.USER
    index = populateEntityUuidPlaceholders(stmt, entityUuids, index);
    // query for RowKind.GROUP
    index = populateEntityUuidPlaceholders(stmt, entityUuids, index);
    // query for RowKind.ANYONE
    index = setPrivateEntityPlaceHolder(stmt, index, false);
    index = populateEntityUuidPlaceholders(stmt, entityUuids, index);
    // query for RowKind.NONE
    index = setPrivateEntityPlaceHolder(stmt, index, true);
    populateEntityUuidPlaceholders(stmt, entityUuids, index);
    return stmt;
  }

  private static int populateEntityUuidPlaceholders(PreparedStatement stmt, List<String> entityUuids, int index) throws SQLException {
    int newIndex = index;
    for (String entityUuid : entityUuids) {
      stmt.setString(newIndex, entityUuid);
      newIndex++;
    }
    return newIndex;
  }

  private static int setPrivateEntityPlaceHolder(PreparedStatement stmt, int index, boolean isPrivate) throws SQLException {
    int newIndex = index;
    stmt.setBoolean(newIndex, isPrivate);
    newIndex++;
    return newIndex;
  }

  private static void processRow(ResultSet rs, Map<String, IndexPermissions> dtosByEntityUuid) throws SQLException {
    RowKind rowKind = RowKind.valueOf(rs.getString(1));
    String entityUuid = rs.getString(2);

    IndexPermissions dto = dtosByEntityUuid.get(entityUuid);
    if (dto == null) {
      String qualifier = rs.getString(5);
      dto = new IndexPermissions(entityUuid, qualifier);
      dtosByEntityUuid.put(entityUuid, dto);
    }
    switch (rowKind) {
      case NONE:
        break;
      case USER:
        dto.addUserUuid(rs.getString(3));
        break;
      case GROUP:
        dto.addGroupUuid(rs.getString(4));
        break;
      case ANYONE:
        dto.allowAnyone();
        break;
    }
  }
}
