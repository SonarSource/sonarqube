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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class CreateBuiltInQualityGate extends DataChange {

  private static final String SONAR_WAY_QUALITY_GATE = "Sonar way";

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public CreateBuiltInQualityGate(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }


  @Override
  protected void execute(Context context) throws SQLException {
    Long nbOfBuiltInQualityGates = context.prepareSelect("select count(uuid) from quality_gates where is_built_in = ?")
      .setBoolean(1, true)
      .get(row -> row.getLong(1));


    if (nbOfBuiltInQualityGates == 0) {
      final Date now = new Date(system2.now());

      context.prepareUpsert("insert into quality_gates (uuid, name, is_built_in, created_at) values (?,?,?,?)")
        .setString(1, uuidFactory.create())
        .setString(2, SONAR_WAY_QUALITY_GATE)
        .setBoolean(3, true)
        .setDate(4, now)
        .execute()
        .commit();
    }
  }
}
