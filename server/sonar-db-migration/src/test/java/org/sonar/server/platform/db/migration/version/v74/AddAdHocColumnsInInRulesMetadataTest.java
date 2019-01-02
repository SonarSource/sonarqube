/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.CLOB;
import static java.sql.Types.TINYINT;
import static java.sql.Types.VARCHAR;

public class AddAdHocColumnsInInRulesMetadataTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(AddAdHocColumnsInInRulesMetadataTest.class, "rules_metadata.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddAdHocColumnsInInRulesMetadata underTest = new AddAdHocColumnsInInRulesMetadata(db.database());

  @Test
  public void columns_are_updated() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("rules_metadata", "ad_hoc_name", VARCHAR, 200, true);
    db.assertColumnDefinition("rules_metadata", "ad_hoc_description", CLOB, null, true);
    db.assertColumnDefinition("rules_metadata", "ad_hoc_severity", VARCHAR, 10, true);
    db.assertColumnDefinition("rules_metadata", "ad_hoc_type", TINYINT, null, true);
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

}
