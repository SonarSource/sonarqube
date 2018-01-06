/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateEventsComponentUuidTest {
  private static final String TABLE_EVENTS = "events";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateEventsComponentUuidTest.class, "events_and_snapshots.sql");

  private final Random random = new Random();

  private PopulateEventsComponentUuid underTest = new PopulateEventsComponentUuid(db.database());

  @Test
  public void execute_has_no_effect_if_table_is_empty() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_deletes_all_events_if_snapshots_is_empty() throws SQLException {
    insertEvent(randomAlphabetic(5), null);
    insertEvent(randomAlphabetic(5), randomAlphabetic(10));

    underTest.execute();

    assertThat(getAllEventUuids()).isEmpty();
  }

  @Test
  public void execute_populates_components_uuid_of_events_which_analysis_exists() throws SQLException {
    String componentUuid = "foo";
    String existingAnalysisUuid = insertSnapshot(componentUuid);
    String missingAnalysisUuid = randomAlphabetic(5);
    String uuid = insertEvent(existingAnalysisUuid, null);
    insertEvent(missingAnalysisUuid, null);

    underTest.execute();

    assertThat(getAllEventUuids()).containsOnly(uuid);
    assertThat(getComponentUuidOf(uuid)).isEqualTo(componentUuid);
  }

  @Test
  public void execute_fixes_inconsistent_component_uuid_of_event_which_analysis_exists() throws SQLException {
    String analysisUuid1 = insertSnapshot("foo");
    String analysisUuid2 = insertSnapshot("bar");
    String eventUuid1 = insertEvent(analysisUuid1, "foo");
    String eventUuid2 = insertEvent(analysisUuid2, "moh");

    underTest.execute();

    assertThat(getAllEventUuids()).containsOnly(eventUuid1, eventUuid2);
    assertThat(getComponentUuidOf(eventUuid1)).isEqualTo("foo");
    assertThat(getComponentUuidOf(eventUuid2)).isEqualTo("bar");
  }

  @Test
  public void execute_deletes_events_without_component_uuid_which_analysis_does_not_exist() throws SQLException {
    String analysisUuid1 = insertSnapshot("foo");
    String eventUuid1 = insertEvent(analysisUuid1, "foo");
    insertEvent(randomAlphabetic(3), "moh");
    insertEvent(randomAlphabetic(3), null);

    underTest.execute();

    assertThat(getAllEventUuids()).containsOnly(eventUuid1);
  }

  private String insertSnapshot(String componentUuid) {
    String uuid = randomAlphabetic(10);
    db.executeInsert(
      "snapshots",
      "UUID", uuid,
      "COMPONENT_UUID", componentUuid,
      "STATUS", "U",
      "ISLAST", String.valueOf(true));
    return uuid;
  }

  private String insertEvent(String analysisUuid, @Nullable String componentUuid) {
    String uuid = randomAlphabetic(5);
    db.executeInsert(
      TABLE_EVENTS,
      "UUID", uuid,
      "ANALYSIS_UUID", analysisUuid,
      "COMPONENT_UUID", componentUuid,
      "EVENT_DATE", random.nextInt(),
      "CREATED_AT", random.nextInt());
    return uuid;
  }

  private Stream<String> getAllEventUuids() {
    return db.select("select uuid as \"UUID\" from events").stream().map(row -> (String) row.get("UUID"));
  }

  private String getComponentUuidOf(String eventUuid) {
    return (String) db.selectFirst("select component_uuid as \"COMPONENT_UUID\" from events where uuid = '" + eventUuid + "'")
        .get("COMPONENT_UUID");
  }
}
