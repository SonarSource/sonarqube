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
package org.sonar.server.db.migrations.v50;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import com.google.common.collect.Lists;

// TODO could be refactored to benefit from existing code of ReplaceIssueFiltersProjectKeyByUuid
// -> make any change of issue_filters easier
public class RemoveSortFieldFromIssueFiltersMigrationStep extends BaseDataChange {

  private static final char FIELD_SEPARATOR = '|';
  private static final String SORT_KEY = "sort=";
  private static final String ASC_KEY = "asc=";

  private final System2 system;

  public RemoveSortFieldFromIssueFiltersMigrationStep(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id,data from issue_filters where data like '%" + SORT_KEY + "%' or data like '%" + ASC_KEY + "%'");
    massUpdate.update("update issue_filters set data=?, updated_at=? where id=?");
    massUpdate.rowPluralName("issue filters");
    massUpdate.execute(new FilterHandler(new Date(system.now())));
  }

  private static class FilterHandler implements MassUpdate.Handler {
    private final Date now;

    private FilterHandler(Date now) {
      this.now = now;
    }

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      String data = row.getNullableString(2);
      String[] fields = StringUtils.split(data, FIELD_SEPARATOR);

      boolean found = false;
      List<String> fieldsToKeep = Lists.newArrayList();
      for (String field : fields) {
        if (field.startsWith(SORT_KEY) || field.startsWith(ASC_KEY)) {
          found = true;
        } else {
          fieldsToKeep.add(field);
        }
      }
      if (found) {
        // data without 'sort' field
        update.setString(1, StringUtils.join(fieldsToKeep, FIELD_SEPARATOR));
        update.setDate(2, now);
        update.setLong(3, row.getNullableLong(1));
      }
      return found;
    }
  }
}
