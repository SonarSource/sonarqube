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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import java.util.Base64;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MigrateDeprecatedGithubPrivateKeyToNewKey extends DataChange {

  private static final String OLD_KEY = "sonar.alm.github.app.privateKey.secured";
  private static final String NEW_KEY = "sonar.alm.github.app.privateKeyContent.secured";

  public MigrateDeprecatedGithubPrivateKeyToNewKey(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Long newPropertyId = context.prepareSelect("select id from properties where prop_key = ?")
      .setString(1, NEW_KEY)
      .get(Select.LONG_READER);
    if (newPropertyId != null) {
      context.prepareUpsert("delete from properties where prop_key = ? ")
        .setString(1, OLD_KEY)
        .execute()
        .commit();
      return;
    }
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id, text_value from properties where prop_key = ?")
      .setString(1, OLD_KEY);
    massUpdate.update("update properties set prop_key = ?, text_value = ? where id = ?");
    massUpdate.execute((row, handler) -> {
      String propertyEncodedInBAse64 = row.getString(2);
      String propertyDecoded = new String(Base64.getDecoder().decode(propertyEncodedInBAse64), UTF_8);
      handler.setString(1, NEW_KEY);
      handler.setString(2, propertyDecoded);
      handler.setLong(3, row.getLong(1));
      return true;
    });
  }

}
