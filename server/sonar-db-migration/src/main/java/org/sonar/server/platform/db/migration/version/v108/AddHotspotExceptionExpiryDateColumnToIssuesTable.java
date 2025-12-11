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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;
import static org.sonar.db.DatabaseUtils.tableColumnExists;


public class AddHotspotExceptionExpiryDateColumnToIssuesTable extends  DdlChange{
    public static final String TABLE_NAME = "issues";
    public static final String HOTSPOT_EXCEPTION_EXPIRY_COLUMN = "hotspot_exception_expires_at";

    public AddHotspotExceptionExpiryDateColumnToIssuesTable(Database db) {
        super(db);
    }

    @Override
    public void execute(Context context) throws SQLException {
        try (var connection = getDatabase().getDataSource().getConnection()) {
            if (!tableColumnExists(connection, TABLE_NAME, HOTSPOT_EXCEPTION_EXPIRY_COLUMN)) {
                var hotspotExceptionExpiry = BigIntegerColumnDef.newBigIntegerColumnDefBuilder()
                        .setColumnName(HOTSPOT_EXCEPTION_EXPIRY_COLUMN)
                        .setIsNullable(true)
                        .build();
                context.execute(
                        new AddColumnsBuilder(getDialect(), TABLE_NAME).addColumn(hotspotExceptionExpiry).build());
            }
        }
    }
}
