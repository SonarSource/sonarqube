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
package org.sonar.server.platform.db.migration.version.v60;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanUsurperRootComponentsTest {

  private static final List<String> TABLES = ImmutableList.of(
    "duplications_index", "project_measures", "ce_activity", "events", "snapshots",
    "project_links", "project_measures", "issues", "file_sources", "group_roles",
    "user_roles", "properties", "widgets", "projects");

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanUsurperRootComponentsTest.class,
    "complete_schema.sql");

  private CleanUsurperRootComponents underTest = new CleanUsurperRootComponents(db.database());

  @Test
  public void migration_has_no_effect_on_empty_db() throws SQLException {
    underTest.execute();

    TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(0));
  }

  @Test
  public void execute_fixes_scope_and_qualifier_of_snapshot_inconsistent_with_component() throws SQLException {
    String[] componentUuids = {
      insertComponent("sc1", "qu1"),
      insertComponent("sc2", "qu2"),
      insertComponent("sc3", "qu3"),
      insertComponent("sc4", "qu4")
    };
    Long[] snapshotIds = {
      insertSnapshot(componentUuids[0], "sc1", "qu1"),
      insertSnapshot(componentUuids[1], "sc2", "quW"),
      insertSnapshot(componentUuids[2], "scX", "qu3"),
      insertSnapshot(componentUuids[3], "scY", "quZ"),
    };

    underTest.execute();

    assertSnapshot(snapshotIds[0], "sc1", "qu1");
    assertSnapshot(snapshotIds[1], "sc2", "qu2");
    assertSnapshot(snapshotIds[2], "sc3", "qu3");
    assertSnapshot(snapshotIds[3], "sc4", "qu4");
  }

  @Test
  public void executes_deletes_usurper_root_components() throws SQLException {
    String[] componentUuids = {
      insertRootComponent(Scopes.PROJECT, Qualifiers.PROJECT),
      insertRootComponent(Scopes.PROJECT, Qualifiers.MODULE),
      insertRootComponent(Scopes.DIRECTORY, Qualifiers.DIRECTORY),
      insertRootComponent(Scopes.FILE, Qualifiers.FILE),
      insertRootComponent(Scopes.PROJECT, Qualifiers.VIEW),
      insertRootComponent(Scopes.PROJECT, Qualifiers.SUBVIEW),
      insertRootComponent(Scopes.FILE, Qualifiers.PROJECT),
      insertRootComponent(Scopes.PROJECT, "DEV"),
      insertRootComponent(Scopes.PROJECT, "DEV_PRJ"),
    };

    underTest.execute();

    assertUuidsInTableProjects("projects", componentUuids[0], componentUuids[4], componentUuids[7]);
  }

  @Test
  public void executes_deletes_data_in_all_children_tables_of_component_for_usurper_root_components() throws SQLException {
    long usurperId = 12L;
    String usurperUuid = "usurper_uuid";
    insertComponent(Scopes.PROJECT, Qualifiers.MODULE, usurperId, usurperUuid, usurperUuid);
    Long snapshotId = insertSnapshot(usurperUuid, usurperUuid);
    insertDuplicationsIndex(snapshotId);
    insertProjectMeasures(usurperUuid, dontCareLong());
    insertCeActivity(usurperUuid, dontCareLong());
    insertEvent(usurperUuid, dontCareLong());
    insertSnapshot(usurperUuid, dontCare());
    insertSnapshot(dontCare(), usurperUuid);
    insertProjectLinks(usurperUuid);
    insertIssue(usurperUuid, null);
    insertIssue(null, usurperUuid);
    insertIssue(usurperUuid, usurperUuid);
    insertFileSource(null, usurperUuid);
    insertFileSource(usurperUuid, null);
    insertFileSource(usurperUuid, usurperUuid);
    insertGroupRole(usurperId);
    insertUserRole(usurperId);
    insertProperties(usurperId);
    insertWidget(usurperId);

    TABLES.stream()
      .forEach(s -> assertThat(db.countRowsOfTable(s)).describedAs("table " + s).isGreaterThanOrEqualTo(1));

    underTest.execute();

    TABLES.stream()
      .forEach(s -> assertThat(db.countRowsOfTable(s)).describedAs("table " + s).isEqualTo(0));
  }

  @Test
  public void execute_deletes_snapshots_which_root_is_not_root() throws SQLException {
    String[] componentUuids = {
      insertRootComponent(Scopes.PROJECT, Qualifiers.PROJECT),
      insertComponent(Scopes.PROJECT, Qualifiers.MODULE),
      insertComponent(Scopes.DIRECTORY, Qualifiers.DIRECTORY),
      insertComponent(Scopes.FILE, Qualifiers.FILE),
      insertComponent(Scopes.PROJECT, Qualifiers.VIEW),
      insertComponent(Scopes.PROJECT, Qualifiers.SUBVIEW),
      insertComponent(Scopes.FILE, Qualifiers.PROJECT),
      insertComponent(Scopes.PROJECT, "DEV"),
      insertComponent(Scopes.PROJECT, "DEV_PRJ"),
    };
    Long[] snapshotIds = {
      insertSnapshot(dontCare(), componentUuids[0]),
      insertSnapshot(dontCare(), componentUuids[1]),
      insertSnapshot(dontCare(), componentUuids[2]),
      insertSnapshot(dontCare(), componentUuids[3]),
      insertSnapshot(dontCare(), componentUuids[4]),
      insertSnapshot(dontCare(), componentUuids[5]),
      insertSnapshot(dontCare(), componentUuids[6]),
      insertSnapshot(dontCare(), componentUuids[7]),
      insertSnapshot(dontCare(), componentUuids[8])
    };

    underTest.execute();

    assertIdsInTableProjects("snapshots", snapshotIds[0], snapshotIds[4], snapshotIds[7]);
  }

  @Test
  public void execute_deletes_children_tables_of_snapshots_when_root_of_snapshot_is_not_root() throws SQLException {
    String componentUuid = insertComponent(Scopes.FILE, Scopes.FILE);
    Long snapshotId = insertSnapshot(dontCare(), componentUuid);
    insertProjectMeasures(dontCare(), snapshotId);
    insertCeActivity(componentUuid, snapshotId);
    insertEvent(componentUuid, snapshotId);

    underTest.execute();

    TABLES.stream()
      .filter(s1 -> !s1.equals("projects"))
      .forEach(s -> assertThat(db.countRowsOfTable(s)).describedAs("table " + s).isEqualTo(0));
  }

  private void insertDuplicationsIndex(Long snapshotId) {
    db.executeInsert(
      "duplications_index",
      "PROJECT_SNAPSHOT_ID", valueOf(dontCareLong()),
      "SNAPSHOT_ID", valueOf(snapshotId),
      "HASH", dontCare(),
      "INDEX_IN_FILE", valueOf(0),
      "START_LINE", valueOf(0),
      "END_LINE", valueOf(0));

  }

  private void insertProjectMeasures(String componentUuid, Long snapshotId) {
    db.executeInsert(
      "project_measures",
      "METRIC_ID", valueOf(123L),
      "COMPONENT_UUID", componentUuid,
      "SNAPSHOT_ID", valueOf(snapshotId));

  }

  private void insertCeActivity(String componentUuid, Long snapshotId) {
    db.executeInsert(
      "ce_activity",
      "UUID", dontCare(),
      "TASK_TYPE", dontCare(),
      "COMPONENT_UUID", componentUuid,
      "SNAPSHOT_ID", valueOf(snapshotId),
      "STATUS", dontCare(),
      "IS_LAST", "true",
      "IS_LAST_KEY", dontCare(),
      "SUBMITTED_AT", valueOf(121L),
      "CREATED_AT", valueOf(122L),
      "UPDATED_AT", valueOf(123L));

  }

  private void insertEvent(String componentUuid, Long snapshotId) {
    db.executeInsert(
      "events",
      "SNAPSHOT_ID", valueOf(snapshotId),
      "COMPONENT_UUID", componentUuid,
      "CREATED_AT", valueOf(122L),
      "EVENT_DATE", valueOf(123L));

  }

  private Long insertSnapshot(String componentUuid, String rootComponentUuid) {
    Long id = idGenerator++;
    db.executeInsert(
      "snapshots",
      "ID", valueOf(id),
      "COMPONENT_UUID", componentUuid,
      "ROOT_COMPONENT_UUID", rootComponentUuid);

    return id;
  }

  private void insertProjectLinks(String componentUuid) {
    db.executeInsert(
      "project_links",
      "COMPONENT_UUID", componentUuid,
      "HREF", dontCare());

  }

  private void insertIssue(@Nullable String componentUuid, @Nullable String projectUuid) {
    db.executeInsert(
      "issues",
      "COMPONENT_UUID", componentUuid == null ? dontCare() : componentUuid,
      "PROJECT_UUID", projectUuid == null ? dontCare() : projectUuid,
      "KEE", "kee_" + componentUuid + projectUuid,
      "MANUAL_SEVERITY", valueOf(true));

  }

  private void insertFileSource(@Nullable String fileUuid, @Nullable String projectUuid) {
    db.executeInsert(
      "file_sources",
      "FILE_UUID", fileUuid == null ? dontCare() : fileUuid,
      "PROJECT_UUID", projectUuid == null ? dontCare() : projectUuid,
      "CREATED_AT", valueOf(122L),
      "UPDATED_AT", valueOf(123L));

  }

  private void insertGroupRole(long componentId) {
    db.executeInsert(
      "group_roles",
      "RESOURCE_ID", valueOf(componentId),
      "ROLE", dontCare());

  }

  private void insertUserRole(long componentId) {
    db.executeInsert(
      "user_roles",
      "RESOURCE_ID", valueOf(componentId),
      "ROLE", dontCare());

  }

  private void insertProperties(long componentId) {
    db.executeInsert(
      "properties",
      "RESOURCE_ID", valueOf(componentId));

  }

  private void insertWidget(long componentId) {
    db.executeInsert(
      "widgets",
      "DASHBOARD_ID", valueOf(95),
      "WIDGET_KEY", dontCare(),
      "RESOURCE_ID", valueOf(componentId));

  }

  private long idGenerator = 0;

  private String insertComponent(String scope, String qualifier) {
    long id = idGenerator++;
    String uuid = "uuid_" + id;
    return insertComponent(scope, qualifier, id, uuid, dontCare());
  }

  private String insertRootComponent(String scope, String qualifier) {
    long id = idGenerator++;
    String uuid = "uuid_" + id;
    return insertComponent(scope, qualifier, id, uuid, uuid);
  }

  private String insertComponent(String scope, String qualifier, long id, String uuid, String projectUuid) {
    db.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid,
      "PROJECT_UUID", projectUuid,
      "SCOPE", scope,
      "QUALIFIER", qualifier);

    return uuid;
  }

  private Long insertSnapshot(String componentUuid, String scope, String qualifier) {
    long id = idGenerator++;

    db.executeInsert(
      "snapshots",
      "id", valueOf(id),
      "component_uuid", componentUuid,
      "root_component_uuid", dontCare(),
      "scope", scope,
      "qualifier", qualifier);

    return id;
  }

  private void assertSnapshot(Long snapshotId, String scope, String qualifier) {
    List<Map<String, Object>> rows = db.select("select SCOPE, QUALIFIER from snapshots where ID=" + snapshotId);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("SCOPE")).isEqualTo(scope);
    assertThat(row.get("QUALIFIER")).isEqualTo(qualifier);
  }

  private void assertIdsInTableProjects(String tableName, Long... expected) {
    assertThat(db.select("select id from " + tableName)
      .stream()
      .map(stringObjectMap -> (Long) stringObjectMap.entrySet().iterator().next().getValue()))
        .containsOnly(expected);
  }

  private void assertUuidsInTableProjects(String tableName, String... expected) {
    assertThat(db.select("select uuid from " + tableName)
      .stream()
      .map(stringObjectMap -> (String) stringObjectMap.entrySet().iterator().next().getValue()))
        .containsOnly(expected);
  }

  private long dontCareGenerator = 0;

  private String dontCare() {
    return "DC_" + dontCareGenerator++;
  }

  private Long dontCareLong() {
    return dontCareGenerator++;
  }
}
