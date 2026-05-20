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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.step.DropIndexChange;

/**
 * Oracle reuses this unique index as the backing index for {@code PK_RULES_DEFAULT_IMPACTS}, so dropping
 * it would break the primary key. On Oracle this step is a no-op; the index is renamed instead by
 * {@link RenameIndexOnRulesDefaultImpactsToPk}.
 */
public class DropUniqueIndexOnRulesDefaultImpacts extends DropIndexChange {
  private static final String TABLE_NAME = "rules_default_impacts";
  private static final String INDEX_NAME = "uniq_rul_uuid_sof_qual";

  public DropUniqueIndexOnRulesDefaultImpacts(Database db) {
    super(db, INDEX_NAME, TABLE_NAME);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (Oracle.ID.equals(getDialect().getId())) {
      return;
    }
    super.execute(context);
  }
}
