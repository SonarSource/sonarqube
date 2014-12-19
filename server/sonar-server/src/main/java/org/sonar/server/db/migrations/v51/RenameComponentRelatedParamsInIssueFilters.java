/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations.v51;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class RenameComponentRelatedParamsInIssueFilters extends BaseDataChange {

  private static final char FIELD_SEPARATOR = '|';
  private static final String COMPONENTS = "components=";
  private static final String PROJECTS = "projects=";
  private static final String COMPONENT_ROOTS = "componentRoots=";
  private static final String COMPONENT_ROOT_UUIDS = "componentRootUuids=";

  private final System2 system;

  public RenameComponentRelatedParamsInIssueFilters(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final Date now = new Date(system.now());
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id,data from issue_filters where " +
      "data like '%" + COMPONENTS + "%' or " +
      "data like '%" + PROJECTS + "%' or " +
      "data like '%" + COMPONENT_ROOTS + "%' or " +
      "data like '%" + COMPONENT_ROOT_UUIDS + "%'");
    massUpdate.update("update issue_filters set data=?, updated_at=? where id=?");
    massUpdate.rowPluralName("issue filters");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        String data = row.getString(2);
        String[] fields = StringUtils.split(data, FIELD_SEPARATOR);

        List<String> fieldsToKeep = Lists.newArrayList();
        for (String field : fields) {
          if (field.startsWith(COMPONENTS) || field.startsWith(PROJECTS) || field.startsWith(COMPONENT_ROOTS) || field.startsWith(COMPONENT_ROOT_UUIDS)) {
            fieldsToKeep.add(
              field.replace(COMPONENTS, "componentKeys=")
                .replace(PROJECTS, "projectKeys=")
                .replace(COMPONENT_ROOTS, "moduleKeys=")
                .replace(COMPONENT_ROOT_UUIDS, "moduleUuids="));
          } else {
            fieldsToKeep.add(field);
          }
        }
        update.setString(1, StringUtils.join(fieldsToKeep, FIELD_SEPARATOR));
        update.setDate(2, now);
        update.setLong(3, row.getLong(1));
        return true;
      }
    });
  }
}
