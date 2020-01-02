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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class RemoveNewsboxDismissHotspotsPropertyTest {

  private static final String USER_PROPERTIES_TABLE_NAME = "user_properties";
  private static final int TOTAL_NUMBER_OF_HOTSPOTS_DISMISS_USER_PROPERTIES = 10;

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(RemoveNewsboxDismissHotspotsPropertyTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();

  private RemoveNewsboxDismissHotspotsProperty underTest = new RemoveNewsboxDismissHotspotsProperty(dbTester.database());

  @Before
  public void setup() {
    insertUserProperty("some-user-uuid", "some-property", random.nextBoolean());

    for (int i = 1; i <= TOTAL_NUMBER_OF_HOTSPOTS_DISMISS_USER_PROPERTIES; i++) {
      insertUserProperty(format("user-uuid-%s", i), "newsbox.dismiss.hotspots", random.nextBoolean());
    }

    int propertiesCount = dbTester.countRowsOfTable(USER_PROPERTIES_TABLE_NAME);
    assertEquals(TOTAL_NUMBER_OF_HOTSPOTS_DISMISS_USER_PROPERTIES + 1, propertiesCount);
  }

  @Test
  public void remove_newsbox_dismiss_hotspot_property() throws SQLException {
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
    int hotspotDismissPropertiesCount = dbTester.countSql("select count(uuid) from user_properties where kee = 'newsbox.dismiss.hotspots'");
    assertEquals(0, hotspotDismissPropertiesCount);
    int otherPropertiesCount = dbTester.countSql("select count(uuid) from user_properties where kee != 'newsbox.dismiss.hotspots'");
    assertEquals(1, otherPropertiesCount);
  }

  private void insertUserProperty(String userUuid, String key, boolean value) {
    dbTester.executeInsert(USER_PROPERTIES_TABLE_NAME,
      "uuid", UUID.randomUUID().toString(),
      "kee", key,
      "user_uuid", userUuid,
      "text_value", value,
      "created_at", Instant.now().toEpochMilli(),
      "updated_at", Instant.now().toEpochMilli());
  }
}
