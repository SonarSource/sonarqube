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
package org.sonar.db.version.v564;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanUsurperRootComponentsTest {

  private static final List<String> TABLES = ImmutableList.of(
    "duplications_index", "project_measures", "ce_activity", "events", "snapshots",
    "project_links", "project_measures", "issues", "file_sources", "group_roles",
    "user_roles", "properties", "widgets", "projects");

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, CleanUsurperRootComponentsTest.class,
    "complete_schema.sql");

  private CleanUsurperRootComponents underTest = new CleanUsurperRootComponents(db.database());

  @Test
  public void migration_has_no_effect_on_empty_db() throws SQLException {
    underTest.execute();

    TABLES.forEach(tableName -> assertThat(db.countRowsOfTable(tableName)).isEqualTo(0));
  }

  @Test
  public void execute_fixes_scope_and_qualifier_of_snapshot_inconsistent_with_component() throws SQLException {
    long[] componentIds = {
      parseLong(insertComponent("sc1", "qu1")),
      parseLong(insertComponent("sc2", "qu2")),
      parseLong(insertComponent("sc3", "qu3")),
      parseLong(insertComponent("sc4", "qu4"))
    };
    Long[] snapshotIds = {
      insertSnapshot(componentIds[0], "sc1", "qu1"),
      insertSnapshot(componentIds[1], "sc2", "quW"),
      insertSnapshot(componentIds[2], "scX", "qu3"),
      insertSnapshot(componentIds[3], "scY", "quZ"),
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
    Long snapshotId = insertSnapshot(usurperId, usurperId);
    insertDuplicationsIndex(snapshotId);
    insertProjectMeasures(usurperId, dontCareLong());
    insertCeActivity(usurperUuid, dontCareLong());
    insertEvent(usurperUuid, dontCareLong());
    insertSnapshot(usurperId, dontCare());
    insertSnapshot(dontCare(), usurperId);
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

    TABLES.forEach(s -> assertThat(db.countRowsOfTable(s)).describedAs("table " + s).isGreaterThanOrEqualTo(1));

    underTest.execute();

    TABLES.forEach(s -> assertThat(db.countRowsOfTable(s)).describedAs("table " + s).isEqualTo(0));
  }

  @Test
  public void execute_deletes_snapshots_which_root_is_not_root() throws SQLException {
    long[] componentIds = {
      parseLong(insertRootComponent(Scopes.PROJECT, Qualifiers.PROJECT)),
      parseLong(insertComponent(Scopes.PROJECT, Qualifiers.MODULE)),
      parseLong(insertComponent(Scopes.DIRECTORY, Qualifiers.DIRECTORY)),
      parseLong(insertComponent(Scopes.FILE, Qualifiers.FILE)),
      parseLong(insertComponent(Scopes.PROJECT, Qualifiers.VIEW)),
      parseLong(insertComponent(Scopes.PROJECT, Qualifiers.SUBVIEW)),
      parseLong(insertComponent(Scopes.FILE, Qualifiers.PROJECT)),
      parseLong(insertComponent(Scopes.PROJECT, "DEV")),
      parseLong(insertComponent(Scopes.PROJECT, "DEV_PRJ"))
    };
    Long[] snapshotIds = {
      insertSnapshot(dontCare(), componentIds[0]),
      insertSnapshot(dontCare(), componentIds[1]),
      insertSnapshot(dontCare(), componentIds[2]),
      insertSnapshot(dontCare(), componentIds[3]),
      insertSnapshot(dontCare(), componentIds[4]),
      insertSnapshot(dontCare(), componentIds[5]),
      insertSnapshot(dontCare(), componentIds[6]),
      insertSnapshot(dontCare(), componentIds[7]),
      insertSnapshot(dontCare(), componentIds[8])
    };

    underTest.execute();

    assertIdsInTableProjects("snapshots", snapshotIds[0], snapshotIds[4], snapshotIds[7]);
  }

  @Test
  public void execute_deletes_children_tables_of_snapshots_when_root_of_snapshot_is_not_root() throws SQLException {
    long usurperId = 12L;
    String componentUuid = insertComponent(Scopes.FILE, Scopes.FILE, usurperId, "U1", "U2");
    Long snapshotId = insertSnapshot(dontCare(), usurperId);
    insertProjectMeasures(usurperId, snapshotId);
    insertCeActivity(componentUuid, snapshotId);
    insertEvent(componentUuid, snapshotId);

    underTest.execute();

    TABLES.stream()
      .filter(s1 -> !s1.equals("projects"))
      .forEach(s -> assertThat(db.countRowsOfTable(s)).describedAs("table " + s).isEqualTo(0));
  }

  @Test
  public void SELECT_sql_should_ignore_collations_on_mssql() {
    String sql = CleanUsurperRootComponents.fixSqlConditions(new MsSql(), "select * from foo where a %s = b %s");

    assertThat(sql).isEqualTo("select * from foo where a collate database_default = b collate database_default");
  }

  @Test
  public void SELECT_sql_should_not_contain_mssql_hints_on_non_mssql() {
    assertSqlIsSanitizedOnNonMssql(new Oracle());
    assertSqlIsSanitizedOnNonMssql(new PostgreSql());
    assertSqlIsSanitizedOnNonMssql(new MySql());
  }

  private static void assertSqlIsSanitizedOnNonMssql(Dialect dialect) {
    String sql = CleanUsurperRootComponents.fixSqlConditions(dialect, "select * from foo where a %s = b %s");
    assertThat(sql).isEqualTo("select * from foo where a  = b ");
  }

  private void insertDuplicationsIndex(long snapshotId) {
    db.executeInsert(
      "duplications_index",
      "PROJECT_SNAPSHOT_ID", valueOf(dontCareLong()),
      "SNAPSHOT_ID", valueOf(snapshotId),
      "HASH", dontCare(),
      "INDEX_IN_FILE", valueOf(0),
      "START_LINE", valueOf(0),
      "END_LINE", valueOf(0));
    db.commit();
  }

  private void insertProjectMeasures(long componentId, long snapshotId) {
    db.executeInsert(
      "project_measures",
      "METRIC_ID", valueOf(123L),
      "PROJECT_ID", componentId,
      "SNAPSHOT_ID", valueOf(snapshotId));
    db.commit();
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
    db.commit();
  }

  private void insertEvent(String componentUuid, Long snapshotId) {
    db.executeInsert(
      "events",
      "SNAPSHOT_ID", valueOf(snapshotId),
      "COMPONENT_UUID", componentUuid,
      "CREATED_AT", valueOf(122L),
      "EVENT_DATE", valueOf(123L));
    db.commit();
  }

  private Long insertSnapshot(long componentId, long rootComponentId) {
    Long id = idGenerator++;
    db.executeInsert(
      "snapshots",
      "ID", valueOf(id),
      "PROJECT_ID", componentId,
      "ROOT_PROJECT_ID", rootComponentId);
    db.commit();
    return id;
  }

  private void insertProjectLinks(String componentUuid) {
    db.executeInsert(
      "project_links",
      "COMPONENT_UUID", componentUuid,
      "HREF", dontCare());
    db.commit();
  }

  private void insertIssue(@Nullable String componentUuid, @Nullable String projectUuid) {
    db.executeInsert(
      "issues",
      "COMPONENT_UUID", componentUuid == null ? dontCare() : componentUuid,
      "PROJECT_UUID", projectUuid == null ? dontCare() : projectUuid,
      "KEE", "kee_" + componentUuid + projectUuid,
      "MANUAL_SEVERITY", valueOf(true));
    db.commit();
  }

  private void insertFileSource(@Nullable String fileUuid, @Nullable String projectUuid) {
    db.executeInsert(
      "file_sources",
      "FILE_UUID", fileUuid == null ? dontCare() : fileUuid,
      "PROJECT_UUID", projectUuid == null ? dontCare() : projectUuid,
      "CREATED_AT", valueOf(122L),
      "UPDATED_AT", valueOf(123L));
    db.commit();
  }

  private void insertGroupRole(long componentId) {
    db.executeInsert(
      "group_roles",
      "RESOURCE_ID", valueOf(componentId),
      "ROLE", dontCare());
    db.commit();
  }

  private void insertUserRole(long componentId) {
    db.executeInsert(
      "user_roles",
      "RESOURCE_ID", valueOf(componentId),
      "ROLE", dontCare());
    db.commit();
  }

  private void insertProperties(long componentId) {
    db.executeInsert(
      "properties",
      "RESOURCE_ID", valueOf(componentId));
    db.commit();
  }

  private void insertWidget(long componentId) {
    db.executeInsert(
      "widgets",
      "DASHBOARD_ID", valueOf(95),
      "WIDGET_KEY", dontCare(),
      "RESOURCE_ID", valueOf(componentId));
    db.commit();
  }

  private long idGenerator = 0;

  private String insertComponent(String scope, String qualifier) {
    long id = idGenerator++;
    String uuid = "" + id;
    return insertComponent(scope, qualifier, id, uuid, String.valueOf(dontCare()));
  }

  private String insertRootComponent(String scope, String qualifier) {
    long id = idGenerator++;
    String uuid = "" + id;
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
    db.commit();
    return uuid;
  }

  private Long insertSnapshot(long componentId, String scope, String qualifier) {
    long id = idGenerator++;

    db.executeInsert(
      "snapshots",
      "id", valueOf(id),
      "project_id", componentId,
      "root_project_id", dontCare(),
      "scope", scope,
      "qualifier", qualifier);
    db.commit();
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

  private long dontCare() {
    return 999_999_999L + dontCareGenerator++;
  }

  private Long dontCareLong() {
    return dontCareGenerator++;
  }
}
