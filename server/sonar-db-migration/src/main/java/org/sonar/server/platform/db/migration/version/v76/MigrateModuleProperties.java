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
package org.sonar.server.platform.db.migration.version.v76;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Upsert;

@SupportsBlueGreen
public class MigrateModuleProperties extends DataChange {

  protected static final String NEW_PROPERTY_NAME = "sonar.subproject.settings.archived";

  private final System2 system2;

  public MigrateModuleProperties(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();
    moveModulePropertiesToProjectLevel(context, now);
    removeModuleProperties(context);
  }

  private static void moveModulePropertiesToProjectLevel(Context context, long time) throws SQLException {
    StringBuilder builder = new StringBuilder();
    AtomicReference<Integer> currentProjectId = new AtomicReference<>();
    AtomicReference<String> currentModuleUuid = new AtomicReference<>();

    context.prepareSelect("select prop.prop_key, prop.text_value, prop.clob_value, module1.name, module1.uuid, root.id as project_id, root.name as project_name " +
      "from properties prop " +
      "left join projects module1 on module1.id = prop.resource_id " +
      "left join projects root on root.uuid = module1.project_uuid " +
      "where module1.qualifier = 'BRC' and prop.user_id is null " +
      "order by root.uuid, module1.uuid, prop.prop_key")
      .scroll(row -> {
        String propertyKey = row.getString(1);
        String propertyTextValue = row.getString(2);
        String propertyClobValue = row.getString(3);
        String moduleName = row.getString(4);
        String moduleUuid = row.getString(5);
        Integer projectId = row.getInt(6);
        String projectName = row.getString(7);

        if (!projectId.equals(currentProjectId.get())) {
          if (currentProjectId.get() != null) {
            insertProjectProperties(context, currentProjectId.get(), builder.toString(), time);
          }

          builder.setLength(0);
          currentProjectId.set(projectId);
        }

        if (!moduleUuid.equals(currentModuleUuid.get())) {
          if (currentModuleUuid.get() != null && builder.length() != 0) {
            builder.append("\n");
          }
          builder.append("# Settings from '").append(projectName).append("::").append(moduleName).append("'\n");
          currentModuleUuid.set(moduleUuid);
        }

        builder.append(propertyKey).append("=");
        if (StringUtils.isNotBlank(propertyTextValue)) {
          builder.append(propertyTextValue);
        } else if (StringUtils.isNotBlank(propertyClobValue)) {
          builder.append(propertyClobValue);
        }
        builder.append("\n");
      });

    if (builder.length() > 0) {
      insertProjectProperties(context, currentProjectId.get(), builder.toString(), time);
    }
  }

  private static void insertProjectProperties(Context context, int projectId, String content, long time) throws SQLException {
    Upsert upsert = context.prepareUpsert("insert into properties (prop_key, resource_id, is_empty, clob_value, created_at) values (?, ?, ?, ?, ?)");
    upsert.setString(1, NEW_PROPERTY_NAME)
      .setInt(2, projectId)
      .setBoolean(3, false)
      .setString(4, content)
      .setLong(5, time)
      .execute()
      .commit();
  }

  private static void removeModuleProperties(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("module level properties");
    massUpdate.select("select prop.id as property_id " +
      "from properties prop " +
      "left join projects module1 on module1.id = prop.resource_id " +
      "where module1.qualifier = 'BRC'");
    massUpdate.update("delete from properties where id=?");
    massUpdate.execute((row, update) -> {
      update.setInt(1, row.getInt(1));
      return true;
    });
  }
}
