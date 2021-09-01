/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class AddOrganizationUuidColumnToRulesTable extends DdlChange {

    private static final String TABLE = "rules";
    private static final String ORGANIZATION_UUID_INDEX = "rules_organization_uuid";

    private static final VarcharColumnDef ORGANIZATION_UUID = newVarcharColumnDefBuilder()
            .setColumnName("organization_uuid")
            .setIsNullable(true)
            .setLimit(UUID_SIZE)
            .build();

    public AddOrganizationUuidColumnToRulesTable(Database db) {
        super(db);
    }

    @Override
    public void execute(Context context) throws SQLException {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE)
                .addColumn(ORGANIZATION_UUID)
                .build());

        context.execute(new CreateIndexBuilder()
                .setTable(TABLE)
                .setName(ORGANIZATION_UUID_INDEX)
                .addColumn(ORGANIZATION_UUID)
                .build());
    }

}
