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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import java.time.Instant;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.sonar.server.platform.db.migration.version.v81.RenameDaysBeforeDeletingInactiveSLBSetting.NEW_PROPERTY_NAME;
import static org.sonar.server.platform.db.migration.version.v81.RenameDaysBeforeDeletingInactiveSLBSetting.OLD_PROPERTY_NAME;
import static org.sonar.server.platform.db.migration.version.v81.RenameDaysBeforeDeletingInactiveSLBSetting.TABLE;

public class RenameDaysBeforeDeletingInactiveSLBSettingTest {

  private static final int TOTAL_NUMBER_OF_PROJECT_LEVEL_PROPERTIES = 10;

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RenameDaysBeforeDeletingInactiveSLBSettingTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RenameDaysBeforeDeletingInactiveSLBSetting underTest = new RenameDaysBeforeDeletingInactiveSLBSetting(dbTester.database());

  @Before
  public void setup() {
    insertProperty(null, OLD_PROPERTY_NAME, "xyz");
    insertProperty(null, "sonar.unrelated.global.property", "xyz");
    for (long i = 1; i <= TOTAL_NUMBER_OF_PROJECT_LEVEL_PROPERTIES; i++) {
      insertProperty(i, OLD_PROPERTY_NAME, format("xyz-%s", i));
      insertProperty(i, "sonar.unrelated.project.property", format("xyz-%s", i));
    }

    int propertiesCount = dbTester.countRowsOfTable(TABLE);
    assertEquals(2 * TOTAL_NUMBER_OF_PROJECT_LEVEL_PROPERTIES + 2, propertiesCount);
  }

  @Test
  public void migrates_old_prop_key_to_new_prop_key() throws SQLException {
    underTest.execute();

    verifyResult();
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    verifyResult();
  }

  private void verifyResult() {
    int propertiesCount = dbTester.countRowsOfTable(TABLE);
    assertEquals(2 * TOTAL_NUMBER_OF_PROJECT_LEVEL_PROPERTIES + 2, propertiesCount);

    int numberOfPropsWithNewName = dbTester.countSql("select count(*) from " + TABLE + " where prop_key = '" + NEW_PROPERTY_NAME + "'");
    int numberOfPropsWithOldName = dbTester.countSql("select count(*) from " + TABLE + " where prop_key = '" + OLD_PROPERTY_NAME + "'");
    assertThat(numberOfPropsWithNewName).isEqualTo(TOTAL_NUMBER_OF_PROJECT_LEVEL_PROPERTIES + 1);
    assertThat(numberOfPropsWithOldName).isEqualTo(0);
  }

  private void insertProperty(@Nullable Long projectId, String propertyKey, String propertyValue) {
    dbTester.executeInsert(TABLE,
      "prop_key", propertyKey,
      "resource_id", projectId,
      "is_empty", false,
      "text_value", propertyValue,
      "created_at", Instant.now().toEpochMilli());
  }
}
