/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v99;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddOrganizationUuidColumnToAuditsTable extends DdlChange {

    @VisibleForTesting
    static final String TABLE_NAME = "audits";
    @VisibleForTesting
    static final String COLUMN_NAME = "organization_uuid";

    public AddOrganizationUuidColumnToAuditsTable(Database db) {
        super(db);
    }

    @Override
    public void execute(Context context) throws SQLException {
        try (Connection c = getDatabase().getDataSource().getConnection()) {
            if (!DatabaseUtils.tableColumnExists(c, TABLE_NAME, COLUMN_NAME)) {
                context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
                        .addColumn(
                                VarcharColumnDef.newVarcharColumnDefBuilder(COLUMN_NAME)
                                        .setLimit(UUID_SIZE)
                                        .setIsNullable(false)
                                        .build())
                        .build());

                context.execute(new CreateIndexBuilder()
                        .setTable(TABLE_NAME)
                        .setName("audits_organization_created_at")
                        .addColumn(COLUMN_NAME)
                        .addColumn("created_at")
                        .setUnique(false)
                        .build());
            }
        }
    }

}
