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
package org.sonar.server.platform.db.migration.version.v92;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

public class CreateQGateGroupPermissionsTableTest {
    private final static String TABLE_NAME = "qgate_group_permissions";
    private final static String QUALITY_GATE_UUID_INDEX = "qg_groups_uuid_idx";

    @Rule
    public final CoreDbTester db = CoreDbTester.createForSchema(CreateQGateGroupPermissionsTableTest.class, "schema.sql");

    private final CreateQGateGroupPermissionsTable underTest = new CreateQGateGroupPermissionsTable(db.database());

    @Test
    public void migration_should_create_a_table_with_index() throws SQLException {
        db.assertTableDoesNotExist(TABLE_NAME);

        underTest.execute();

        db.assertTableExists(TABLE_NAME);
        db.assertIndex(TABLE_NAME, QUALITY_GATE_UUID_INDEX, "quality_gate_uuid");
    }

    @Test
    public void migration_should_be_reentrant() throws SQLException {
        db.assertTableDoesNotExist(TABLE_NAME);

        underTest.execute();
        //re-entrant
        underTest.execute();

        db.assertTableExists(TABLE_NAME);
        db.assertIndex(TABLE_NAME, QUALITY_GATE_UUID_INDEX, "quality_gate_uuid");
    }
}
