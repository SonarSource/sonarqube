/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

/**
 * 'Anyone' (every user, including anonymous) permissions are stored as rows with a null 'group_uuid'. Every such row
 * of 'group_roles' is migrated to 'sonar-users' (every authenticated user) when not already present, before
 * {@link RemoveAnyoneGroupPermissions} deletes the original rows. 'user' and 'codeviewer' rows on public entities are
 * not migrated, as these permissions are implicit there.
 * <p>
 * 'Anyone' rows of 'perm_templates_groups' are not migrated, only logged: they never applied to private projects,
 * whereas 'sonar-users' template permissions do.
 */
public class MigrateAnyoneGroupPermissionsToSonarUsers extends DataChange {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateAnyoneGroupPermissionsToSonarUsers.class);

  private static final String SONAR_USERS_GROUP = "sonar-users";
  private static final String SELECT_ANYONE_GROUP_ROLES = """
    select gr.entity_uuid, gr.role from group_roles gr
    where gr.group_uuid is null
      and not (
        gr.role in ('user', 'codeviewer')
        and (
          exists (select 1 from projects p where p.uuid = gr.entity_uuid and p.private = ?)
          or exists (select 1 from portfolios pf where pf.uuid = gr.entity_uuid and pf.private = ?)
        )
      )
    """;

  public MigrateAnyoneGroupPermissionsToSonarUsers(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String sonarUsersUuid = selectSonarUsersGroupUuid(context);
    if (sonarUsersUuid == null) {
      LOGGER.warn("'{}' group not found, skipping migration of 'Anyone' group permissions", SONAR_USERS_GROUP);
      return;
    }

    migrateGroupRoles(context, sonarUsersUuid);
    logDroppedTemplatePermissions(context);
  }

  @CheckForNull
  private static String selectSonarUsersGroupUuid(Context context) throws SQLException {
    return context.prepareSelect("select uuid from groups where name = ?")
      .setString(1, SONAR_USERS_GROUP)
      .get(row -> row.getString(1));
  }

  private static void migrateGroupRoles(Context context, String sonarUsersUuid) throws SQLException {
    List<AnyoneGroupRole> anyoneRoles = context.prepareSelect(SELECT_ANYONE_GROUP_ROLES)
      .setBoolean(1, false)
      .setBoolean(2, false)
      .list(row -> new AnyoneGroupRole(row.getString(1), row.getString(2)));
    if (anyoneRoles.isEmpty()) {
      return;
    }

    Set<AnyoneGroupRole> existingSonarUsersRoles = new HashSet<>(
      context.prepareSelect("select entity_uuid, role from group_roles where group_uuid = ?")
        .setString(1, sonarUsersUuid)
        .list(row -> new AnyoneGroupRole(row.getString(1), row.getString(2))));

    try (Upsert upsert = context.prepareUpsert("insert into group_roles (uuid, group_uuid, entity_uuid, role) values (?, ?, ?, ?)")) {
      for (AnyoneGroupRole anyoneRole : anyoneRoles) {
        if (!existingSonarUsersRoles.add(anyoneRole)) {
          continue;
        }
        upsert
          .setString(1, UUID.randomUUID().toString())
          .setString(2, sonarUsersUuid)
          .setString(3, anyoneRole.entityUuid())
          .setString(4, anyoneRole.role())
          .execute();
        LOGGER.warn("Migrated 'Anyone' permission '{}' on entity '{}' to '{}'", anyoneRole.role(), anyoneRole.entityUuid(), SONAR_USERS_GROUP);
      }
      upsert.commit();
    }
  }

  private static void logDroppedTemplatePermissions(Context context) throws SQLException {
    context.prepareSelect("select template_uuid, permission_reference from perm_templates_groups where group_uuid is null")
      .scroll(row -> LOGGER.warn("Dropped 'Anyone' permission '{}' on permission template '{}', it is not migrated to '{}'",
        row.getString(2), row.getString(1), SONAR_USERS_GROUP));
  }

  private record AnyoneGroupRole(@Nullable String entityUuid, String role) {
  }
}
