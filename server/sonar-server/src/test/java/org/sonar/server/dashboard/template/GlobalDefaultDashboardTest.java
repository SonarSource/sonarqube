/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.dashboard.template;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.Dashboard.Widget;
import org.sonar.db.measure.MeasureFilterDao;
import org.sonar.db.measure.MeasureFilterDto;
import org.sonar.server.dashboard.widget.MeasureFilterAsTreemapWidget;
import org.sonar.server.dashboard.widget.MeasureFilterListWidget;
import org.sonar.server.dashboard.widget.WelcomeWidget;
import org.sonar.server.measure.template.MyFavouritesFilter;
import org.sonar.server.measure.template.ProjectFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalDefaultDashboardTest {
  GlobalDefaultDashboard template;
  MeasureFilterDao dao;

  @Before
  public void init() {
    dao = mock(MeasureFilterDao.class);
    template = new GlobalDefaultDashboard(dao);
  }

  @Test
  public void should_have_a_name() {
    assertThat(template.getName()).isEqualTo("Home");
  }

  @Test
  public void should_create_global_dashboard_with_four_widgets() {
    when(dao.selectSystemFilterByName(MyFavouritesFilter.NAME)).thenReturn(
      new MeasureFilterDto().setId(100L)
      );
    when(dao.selectSystemFilterByName(ProjectFilter.NAME)).thenReturn(
      new MeasureFilterDto().setId(101L)
      );
    Dashboard dashboard = template.createDashboard();
    List<Widget> firstColumn = dashboard.getWidgetsOfColumn(1);
    assertThat(firstColumn).hasSize(2);
    assertThat(firstColumn.get(0).getId()).isEqualTo(WelcomeWidget.ID);
    assertThat(firstColumn.get(1).getId()).isEqualTo(MeasureFilterListWidget.ID);
    assertThat(firstColumn.get(1).getProperty("filter")).isEqualTo("100");

    List<Widget> secondColumn = dashboard.getWidgetsOfColumn(2);
    assertThat(secondColumn).hasSize(2);
    assertThat(secondColumn.get(0).getId()).isEqualTo(MeasureFilterListWidget.ID);
    assertThat(secondColumn.get(0).getProperty("filter")).isEqualTo("101");
    assertThat(secondColumn.get(1).getId()).isEqualTo(MeasureFilterAsTreemapWidget.ID);
    assertThat(secondColumn.get(1).getProperty("filter")).isEqualTo("101");
  }

  @Test
  public void should_not_fail_if_filter_widgets_not_found() {
    when(dao.selectSystemFilterByName(MyFavouritesFilter.NAME)).thenReturn(null);
    when(dao.selectSystemFilterByName(ProjectFilter.NAME)).thenReturn(null);

    Dashboard dashboard = template.createDashboard();
    List<Widget> firstColumn = dashboard.getWidgetsOfColumn(1);
    assertThat(firstColumn).hasSize(1);
    assertThat(firstColumn.get(0).getId()).isEqualTo(WelcomeWidget.ID);

    List<Widget> secondColumn = dashboard.getWidgetsOfColumn(2);
    assertThat(secondColumn).isEmpty();
  }
}
