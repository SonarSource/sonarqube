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

import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.CsvWriter;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import java.io.StringWriter;
import java.sql.SQLException;

public class FeedUsersScmAccounts extends BaseDataChange {

  private final System2 system;

  public FeedUsersScmAccounts(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final long now = system.now();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT u.id, u.login, u.email FROM users u WHERE scm_accounts IS NULL");
    massUpdate.update("UPDATE users SET scm_accounts=?, updated_at=? WHERE id=?");
    massUpdate.rowPluralName("users");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getLong(1);
        String login = row.getString(2);
        String email = row.getString(3);

        StringWriter writer = new StringWriter(2);
        CsvWriter csv = CsvWriter.of(writer);
        csv.values(login, email);
        csv.close();

        update.setString(1, writer.toString());
        update.setLong(2, now);
        update.setLong(3, id);
        return true;
      }
    });
  }

}
