/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;



class CreateIndexForRuleImpactChangesTableIT {

  static final String INDEX_NAME = "rule_impact_changes_r_c_uuid";
  static final String TABLE_NAME = "rule_impact_changes";
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIndexForRuleImpactChangesTable.class);
  private final CreateIndexForRuleImpactChangesTable underTest = new CreateIndexForRuleImpactChangesTable(db.database());

  @Test
  void migration_should_create_index() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME, "rule_change_uuid");
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME, "rule_change_uuid");
  }
}
