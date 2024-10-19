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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.DESCRIPTION_SECTION_KEY_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class CreateCvesTableIT {

  private static final String TABLE_NAME = "cves";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateCvesTable.class);

  private final DdlChange createCvesTable = new CreateCvesTable(db.database());
  
  @Test
  void execute_shouldCreateTable() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);
    
    createCvesTable.execute();
    
    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, "uuid", VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "id", VARCHAR, DESCRIPTION_SECTION_KEY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "description", VARCHAR, MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "cvss_score", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE_NAME, "epss_score", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE_NAME, "epss_percentile", Types.DOUBLE, null, true);
    db.assertColumnDefinition(TABLE_NAME, "published_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "last_modified_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "created_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "updated_at", BIGINT, null, false);
    db.assertPrimaryKey(TABLE_NAME, "pk_cves", "uuid");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);
    createCvesTable.execute();

    createCvesTable.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
