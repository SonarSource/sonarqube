/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v51;

import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

public class AddIssuesColumnsTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, AddIssuesColumnsTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() {
    migration = new AddIssuesColumns(db.database());
  }

  @Test
  public void update_columns() throws Exception {
    migration.execute();

    db.assertColumnDefinition("issues", "issue_creation_date_ms", Types.BIGINT, null);
    db.assertColumnDefinition("issues", "issue_update_date_ms", Types.BIGINT, null);
    db.assertColumnDefinition("issues", "issue_close_date_ms", Types.BIGINT, null);
    db.assertColumnDefinition("issues", "tags", Types.VARCHAR, 4000);
    db.assertColumnDefinition("issues", "component_uuid", Types.VARCHAR, 50);
    db.assertColumnDefinition("issues", "project_uuid", Types.VARCHAR, 50);
  }

}
