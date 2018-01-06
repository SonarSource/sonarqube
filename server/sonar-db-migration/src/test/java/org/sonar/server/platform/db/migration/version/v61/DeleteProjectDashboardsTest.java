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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteProjectDashboardsTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteProjectDashboardsTest.class, "schema.sql");

  private DeleteProjectDashboards underTest = new DeleteProjectDashboards(db.database());

  @Test
  public void no_effect_on_empty_tables() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("dashboards")).isEqualTo(0);
  }

  @Test
  public void delete_project_dashboard_data() throws SQLException {
    insertGlobalDashboards(1L, 10L, 11L);
    insertProjectDashboards(2L, 20L, 21L);
    insertActiveDashboards(1L, 10L, 11L, 12L);
    insertActiveDashboards(2L, 20L, 21L, 22L);
    insertWidgets(1L, 100L, 101L, 102L);
    insertWidgets(2L, 200L, 201L, 202L);
    insertWidgetProperties(100L, 1001L, 1002L, 1003L);
    insertWidgetProperties(202L, 2021L, 2022L, 2023L);

    underTest.execute();

    assertIdsOfDashboardsAre(1L, 10L, 11L);
    assertIdsOfActiveDashboardsAre(10L, 11L, 12L);
    assertIdsOfWidgetsAre(100L, 101L, 102L);
    assertIdsOfWidgetPropertiesAre(1001L, 1002L, 1003L);
  }

  @Test
  public void is_reentrant() throws SQLException {
    insertGlobalDashboards(10L, 11L, 12L);
    insertProjectDashboards(20L, 21L, 22L);
    underTest.execute();
    assertIdsOfDashboardsAre(10L, 11L, 12L);

    underTest.execute();
    assertIdsOfDashboardsAre(10L, 11L, 12L);
  }

  private void insertProjectDashboards(long... ids) {
    Arrays.stream(ids).forEach(id -> insertDashboard(id, false));
  }

  private void insertGlobalDashboards(long... ids) {
    Arrays.stream(ids).forEach(id -> insertDashboard(id, true));
  }

  private void insertDashboard(long id, boolean isGlobal) {
    db.executeInsert(
      "dashboards",
      "ID", valueOf(id),
      "IS_GLOBAL", valueOf(isGlobal));
  }

  private void insertActiveDashboards(long dashboardId, long... ids) {
    Arrays.stream(ids).forEach(
      id -> db.executeInsert(
        "active_dashboards",
        "ID", valueOf(id),
        "DASHBOARD_ID", valueOf(dashboardId)));
  }

  private void insertWidgets(long dashboardId, long... ids) {
    Arrays.stream(ids).forEach(
      id -> db.executeInsert(
        "widgets",
        "ID", valueOf(id),
        "WIDGET_KEY", valueOf(id),
        "DASHBOARD_ID", valueOf(dashboardId)));
  }

  private void insertWidgetProperties(long widgetId, long... ids) {
    Arrays.stream(ids).forEach(
      id -> db.executeInsert(
        "widget_properties",
        "ID", valueOf(id),
        "WIDGET_ID", valueOf(widgetId)));
  }

  private void assertIdsOfDashboardsAre(Long... ids) {
    List<Long> idsInDb = db.select("select ID from dashboards").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());

    assertThat(idsInDb).containsOnly(ids);
  }

  private void assertIdsOfActiveDashboardsAre(Long... ids) {
    List<Long> idsInDb = db.select("select ID from active_dashboards").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());

    assertThat(idsInDb).containsOnly(ids);
  }

  private void assertIdsOfWidgetsAre(Long... ids) {
    List<Long> idsInDb = db.select("select ID from widgets").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());

    assertThat(idsInDb).containsOnly(ids);
  }

  private void assertIdsOfWidgetPropertiesAre(Long... ids) {
    List<Long> idsInDb = db.select("select ID from widget_properties").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());

    assertThat(idsInDb).containsOnly(ids);
  }
}
