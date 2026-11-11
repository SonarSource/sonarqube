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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnComponentsKeeLowerHash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MssqlMetadataReaderIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIndexOnComponentsKeeLowerHash.class);

  @Test
  void getDependentComputedColumns_findsKeeLowerHashOnMsSql() throws SQLException {
    assumeTrue(MsSql.ID.equals(db.database().getDialect().getId()));
    new CreateIndexOnComponentsKeeLowerHash(db.database()).execute();

    ColumnDef kee = new ColumnDef("components", "kee", null, null, "nvarchar", 1000, true);
    List<MssqlCharsetHandler.ComputedColumn> columns;
    try (Connection connection = db.database().getDataSource().getConnection()) {
      columns = new MssqlMetadataReader(new SqlExecutor()).getDependentComputedColumns(connection, kee);
    }

    assertThat(columns).extracting(MssqlCharsetHandler.ComputedColumn::getName).containsExactly("kee_lower_hash");
    assertThat(columns.get(0).getDefinition()).containsIgnoringCase("[kee]");
  }
}
