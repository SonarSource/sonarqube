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
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

public class FakeDashboardTemplate extends DashboardTemplate {

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create()
      .setLayout(DashboardLayout.TWO_COLUMNS_30_70)
      .setDescription("Fake dashboard for integration tests");
    dashboard.addWidget("lcom4", 1);
    dashboard.addWidget("description", 1);
    dashboard.addWidget("documentation_comments", 2);
    dashboard.addWidget("complexity", 3); // should be ignored because the layout is 2 columns
    return dashboard;
  }

  @Override
  public String getName() {
    return "Fake";
  }
}
