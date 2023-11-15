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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteRedundantFailedAlertsForApplicationsTest {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DeleteRedundantFailedAlertsForApplications.class);
  private final DataChange underTest = new DeleteRedundantFailedAlertsForApplications(db.database());

  @Before
  public void setUp() {
    // cleanup db
    db.executeUpdateSql("truncate table events");
    db.executeUpdateSql("truncate table event_component_changes");
    db.executeUpdateSql("truncate table components");
  }

  @Test
  public void givenFailedAlertsForApplication_whenExecuted_thenFailedAlertsAreDeleted() throws SQLException {
    // given
    insertComponent("app1", "appUuid1", "appUuid1", "APP");

    // event that should be deleted
    insertEvent("eventUuid1", "appUuid1", "Failed", "Alert", "{ stillFailing: false, status: \"ERROR\" }");
    insertEventChanges("eventChangeUuid1", "eventUuid1", "appUuid1");
    insertEventChanges("eventChangeUuid2", "eventUuid1", "appUuid1");

    // events that should not be deleted
    insertEvent("eventUuid2", "appUuid1", "Passed", "Alert", "{ stillFailing: false, status: \"ERROR\" }");
    insertEventChanges("eventChangeUuid3", "eventUuid2", "appUuid1");
    insertEvent("eventUuid3", "appUuid1", "Failed", "Alert", "{ stillFailing: false, status: \"PASSED\" }");
    insertEventChanges("eventChangeUuid4", "eventUuid3", "appUuid1");

    // when
    underTest.execute();

    // then
    assertThat(db.countRowsOfTable("events")).isEqualTo(2);
    assertThat(db.countSql("select count(1) from events where uuid = 'eventUuid1'")).isZero();

    assertThat(db.countRowsOfTable("event_component_changes")).isEqualTo(2);
    assertThat(db.countSql("select count(1) from event_component_changes where uuid = 'eventUuid1'")).isZero();
  }

  @Test
  public void givenFailedAlertsForProject_whenExecute_thenTheEventsAreNotDeleted() throws SQLException {
    // given
    insertComponent("project1", "projectUuid1", "projectUuid1", "TRK");

    // event that should not be deleted
    insertEvent("eventUuid1", "projectUuid1", "Failed", "Alert", "{ stillFailing: false, status: \"ERROR\" }");
    insertEventChanges("eventChangeUuid1", "eventUuid1", "projectUuid1");
    insertEventChanges("eventChangeUuid2", "eventUuid1", "projectUuid1");

    // when
    underTest.execute();

    // then
    assertThat(db.countSql("select count(1) from events where uuid = 'eventUuid1'")).isEqualTo(1);
    assertThat(db.countSql("select count(1) from event_component_changes where event_uuid = 'eventUuid1'")).isEqualTo(2);
  }

  @Test
  public void givenMigration_whenExecutedMoreThanOnce_thenNoError() throws SQLException {
    // given
    insertComponent("app1", "appUuid1", "appUuid1", "APP");

    // event that should be deleted
    insertEvent("eventUuid1", "appUuid1", "Failed", "Alert", "{ stillFailing: false, status: \"ERROR\" }");
    insertEventChanges("eventChangeUuid1", "eventUuid1", "appUuid1");
    insertEventChanges("eventChangeUuid2", "eventUuid1", "appUuid1");

    // when
    underTest.execute();
    underTest.execute();

    // then
    assertThat(db.countSql("select count(1) from events where uuid = 'eventUuid1'")).isZero();
    assertThat(db.countSql("select count(1) from event_component_changes where uuid = 'eventUuid1'")).isZero();
  }

  private void insertComponent(String key, String uuid, String branchUuid, String qualifier) {
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("KEE", key),
      Map.entry("BRANCH_UUID", branchUuid),
      Map.entry("UUID_PATH", "." + uuid + "."),
      Map.entry("QUALIFIER", qualifier),
      Map.entry("ENABLED", true),
      Map.entry("PRIVATE", true)
    );

    db.executeInsert("components", map);
  }

  private void insertEvent(String uuid, String componentUuid, String name, String category, String eventData) {
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("NAME", name),
      Map.entry("ANALYSIS_UUID", "analysisUuid"),
      Map.entry("CATEGORY", category),
      Map.entry("CREATED_AT", 1_500_000_000_000L),
      Map.entry("EVENT_DATE", 1_500_000_000_000L),
      Map.entry("COMPONENT_UUID", componentUuid),
      Map.entry("EVENT_DATA", eventData)
    );

    db.executeInsert("events", map);
  }

  private void insertEventChanges(String uuid, String eventUuid, String componentUuid) {
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("EVENT_UUID", eventUuid),
      Map.entry("EVENT_COMPONENT_UUID", componentUuid),
      Map.entry("EVENT_ANALYSIS_UUID", "analysisUuid"),
      Map.entry("CHANGE_CATEGORY", "FAILED_QG"),
      Map.entry("COMPONENT_UUID", uuid),
      Map.entry("COMPONENT_KEY", "app"),
      Map.entry("COMPONENT_NAME", "app"),
      Map.entry("COMPONENT_BRANCH_KEY", 1_500_000_000_000L),
      Map.entry("CREATED_AT", 1_500_000_000_000L)
    );

    db.executeInsert("event_component_changes", map);
  }


}
