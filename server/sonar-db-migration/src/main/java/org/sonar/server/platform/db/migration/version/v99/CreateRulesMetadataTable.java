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

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;
import org.sonar.server.platform.db.migration.version.v00.CreateInitialSchema;

/**
 * We re-create the rules_metadata table if it was dropped in DbVersion95.
 */
public class CreateRulesMetadataTable extends CreateTableChange {

    private static final String TABLE_NAME = "rules_metadata";

    protected CreateRulesMetadataTable(Database db) {
        super(db, TABLE_NAME);
    }

    @Override
    public void execute(Context context, String tableName) throws SQLException {
        new CreateInitialSchema(getDatabase()).createRulesMetadata(context);

        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "note_data").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "note_user_uuid").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "note_created_at").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "note_updated_at").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "remediation_function").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "remediation_gap_mult").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "remediation_base_effort").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "tags").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "ad_hoc_name").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "ad_hoc_description").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "ad_hoc_severity").build());
        context.execute(new DropColumnsBuilder(getDatabase().getDialect(), "rules", "ad_hoc_type").build());
    }
}
