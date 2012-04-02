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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.Dashboard.Widget;
import org.sonar.api.web.DashboardLayout;

public class TimeMachineDashboardTest {
  @Test
  public void shouldCreateDashboard() {
    TimeMachineDashboard template = new TimeMachineDashboard();
    Dashboard hotspots = template.createDashboard();
    assertThat(template.getName(), is("TimeMachine"));
    assertThat(hotspots.getLayout(), is(DashboardLayout.TWO_COLUMNS));
    Collection<Widget> widgets = hotspots.getWidgets();
    assertThat(widgets.size(), is(7));
    for (Widget widget : widgets) {
      if (widget.getId().equals("time_machine")) {
        assertThat(widget.getProperty("displaySparkLine"), is("true"));
      }
    }
  }
}
