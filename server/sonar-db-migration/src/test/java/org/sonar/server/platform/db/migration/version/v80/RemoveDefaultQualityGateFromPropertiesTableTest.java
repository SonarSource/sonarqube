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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;


public class RemoveDefaultQualityGateFromPropertiesTableTest {
  private static final String PROPERTIES_TABLE_NAME = "properties";
  private static final int TOTAL_NUMBER_OF_PROPERTIES = 10;

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RemoveDefaultQualityGateFromPropertiesTableTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RemoveDefaultQualityGateFromPropertiesTable underTest = new RemoveDefaultQualityGateFromPropertiesTable(dbTester.database());

  @Test
  public void remove_default_quality_gate_property() throws SQLException {
    for (long i = 1; i <= TOTAL_NUMBER_OF_PROPERTIES; i++) {
      insertQualityGateProperty(i, i + 100);
    }

    int propertiesCount = dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME);
    Assert.assertEquals(TOTAL_NUMBER_OF_PROPERTIES, propertiesCount);

    underTest.execute();

    //should delete properties
    propertiesCount = dbTester.countRowsOfTable(PROPERTIES_TABLE_NAME);
    Assert.assertEquals(0, propertiesCount);

    //should not fail if executed twice
    underTest.execute();
  }

  private void insertQualityGateProperty(Long projectId, Long qualityGateId) {
    dbTester.executeInsert(PROPERTIES_TABLE_NAME,
      "prop_key", "sonar.qualitygate",
      "resource_id", projectId,
      "is_empty", false,
      "text_value", Long.toString(qualityGateId),
      "created_at", Instant.now().toEpochMilli());
  }
}
