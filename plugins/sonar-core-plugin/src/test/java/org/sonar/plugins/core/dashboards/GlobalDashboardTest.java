/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.dashboards;

import org.junit.Test;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;

import static org.fest.assertions.Assertions.assertThat;

public class GlobalDashboardTest {
  @Test
  public void should_create_dashboard() {
    GlobalDashboard template = new GlobalDashboard();

    Dashboard dashboard = template.createDashboard();

    assertThat(template.getName()).isEqualTo("Global");
    assertThat(dashboard.getLayout()).isEqualTo(DashboardLayout.ONE_COLUMN);
    assertThat(dashboard.getWidgets()).onProperty("id").containsOnly("global", "image");
  }
}
