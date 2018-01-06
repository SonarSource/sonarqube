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
package org.sonar.api.web;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardTest {

  @Test
  public void shouldCreateDashboard() {
    Dashboard dashboard = Dashboard.create();
    assertThat(dashboard.getLayout()).isEqualTo(DashboardLayout.TWO_COLUMNS);
    assertThat(dashboard.getDescription()).isNull();
    assertThat(dashboard.getWidgets()).hasSize(0);
  }

  @Test
  public void shouldAddWidgets() {
    Dashboard dashboard = Dashboard.create();
    Dashboard.Widget mostViolatedRules = dashboard.addWidget("most_violated_rules", 1);
    assertThat(mostViolatedRules.getId()).isEqualTo("most_violated_rules");
    assertThat(dashboard.getWidgets()).hasSize(1);
    assertThat(dashboard.getWidgetsOfColumn(1)).hasSize(1);

    dashboard.addWidget("hotspots", 1);
    assertThat(dashboard.getWidgets()).hasSize(2);
    assertThat(dashboard.getWidgetsOfColumn(1)).hasSize(2);

    // widgets are sorted by order of insertion
    assertThat(dashboard.getWidgetsOfColumn(1).get(1).getId()).isEqualTo("hotspots");
  }

  @Test
  public void shouldAddWidgetsOnDifferentColumns() {
    Dashboard dashboard = Dashboard.create();

    dashboard.addWidget("most_violated_rules", 1);
    assertThat(dashboard.getWidgets().size()).isEqualTo(1);
    assertThat(dashboard.getWidgetsOfColumn(1).size()).isEqualTo(1);

    dashboard.addWidget("hotspots", 2);
    assertThat(dashboard.getWidgets().size()).isEqualTo(2);
    assertThat(dashboard.getWidgetsOfColumn(2).size()).isEqualTo(1);
  }

  @Test
  public void shouldAddSeveralTimesTheSameWidget() {
    Dashboard dashboard = Dashboard.create();
    dashboard.addWidget("most_violated_rules", 1);
    dashboard.addWidget("most_violated_rules", 1).setProperty("foo", "bar");

    assertThat(dashboard.getWidgets().size()).isEqualTo(2);
    assertThat(dashboard.getWidgetsOfColumn(1).get(0).getProperties().size()).isEqualTo(0);
    assertThat(dashboard.getWidgetsOfColumn(1).get(1).getProperty("foo")).isEqualTo("bar");
  }

  @Test
  public void shouldSetWidgetProperty() {
    Dashboard dashboard = Dashboard.create();
    Dashboard.Widget widget = dashboard.addWidget("fake-widget", 1);
    widget.setProperty("foo", "bar");

    assertThat(widget.getProperties().get("foo")).isEqualTo("bar");
  }
}
