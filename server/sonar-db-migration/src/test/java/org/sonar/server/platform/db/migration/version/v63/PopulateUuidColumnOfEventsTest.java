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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateUuidColumnOfEventsTest {

  private static final String TABLE_EVENTS = "events";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateUuidColumnOfEventsTest.class, "in_progress_events.sql");

  private PopulateUuidColumnOfEvents underTest = new PopulateUuidColumnOfEvents(db.database(), UuidFactoryImpl.INSTANCE);

  @Test
  public void migration_has_no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_EVENTS)).isEqualTo(0);
  }

  @Test
  public void migration_generates_uuids() throws SQLException {
    insertEvents(1);
    insertEvents(2);
    insertEvents(3);

    underTest.execute();

    verifyUuids(3);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertEvents(1);

    underTest.execute();
    verifyUuids(1);

    underTest.execute();
    verifyUuids(1);
  }

  private void verifyUuids(int expectedCount) {
    List<Map<String, Object>> rows = db.select("select uuid from events where uuid is not null");
    Set<Object> uuids = rows.stream().map(cols -> cols.get("UUID")).filter(uuid -> StringUtils.isNotBlank((String) uuid)).collect(Collectors.toSet());
    assertThat(uuids).hasSize(expectedCount);
  }

  private String insertEvents(long id) {
    String uuid = "uuid_" + id;
    db.executeInsert(
      TABLE_EVENTS,
      "ID", valueOf(id),
      "COMPONENT_UUID", valueOf(id + 10),
      "ANALYSIS_UUID", valueOf(id + 100),
      "EVENT_DATE", 123456,
      "CREATED_AT", 456789);
    return uuid;
  }

}
