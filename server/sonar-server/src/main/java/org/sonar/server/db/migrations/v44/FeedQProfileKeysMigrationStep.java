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

package org.sonar.server.db.migrations.v44;

import java.sql.SQLException;

import org.apache.commons.lang.RandomStringUtils;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;
import org.sonar.server.util.Slug;

/**
 * Feed the new columns RULES_PROFILES.KEE and PARENT_KEE.
 * 
 * @since 4.4
 */
public class FeedQProfileKeysMigrationStep extends BaseDataChange {

  public FeedQProfileKeysMigrationStep(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    updateKeys(context);
    updateParentKeys(context);
  }

  private void updateKeys(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT id,language,name FROM rules_profiles");
    massUpdate.update("UPDATE rules_profiles SET kee=? WHERE id=?");
    massUpdate.rowPluralName("profiles");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);
        String lang = row.getNullableString(2);
        String name = row.getNullableString(3);

        update.setString(1, Slug.slugify(String.format("%s %s %s", lang, name, RandomStringUtils.randomNumeric(5))));
        update.setLong(2, id);
        return true;
      }
    });
  }

  private void updateParentKeys(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT child.id,parent.kee FROM rules_profiles child, rules_profiles parent WHERE child.parent_name=parent.name " +
      "and child.language=parent.language AND child.parent_name IS NOT NULL");
    massUpdate.update("UPDATE rules_profiles SET parent_kee=? WHERE id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);
        String parentKey = row.getNullableString(2);

        update.setString(1, parentKey);
        update.setLong(2, id);
        return true;
      }
    });

  }
}
