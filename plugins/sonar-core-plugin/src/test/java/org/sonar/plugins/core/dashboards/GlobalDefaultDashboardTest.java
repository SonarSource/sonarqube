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
package org.sonar.plugins.core.dashboards;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.Dashboard.Widget;
import org.sonar.core.measure.db.MeasureFilterDao;
import org.sonar.core.measure.db.MeasureFilterDto;
import org.sonar.plugins.core.CorePlugin;
import org.sonar.plugins.core.measurefilters.MyFavouritesFilter;
import org.sonar.plugins.core.measurefilters.ProjectFilter;
import org.sonar.plugins.core.widgets.WelcomeWidget;
import org.sonar.plugins.core.widgets.measures.MeasureFilterAsTreemapWidget;
import org.sonar.plugins.core.widgets.measures.MeasureFilterListWidget;

import java.util.List;

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
  public void should_be_registered_as_an_extension() {
    assertThat(new CorePlugin().getExtensions()).contains(template.getClass());
  }

  @Test
  public void should_create_global_dashboard_with_four_widgets() {
    when(dao.findSystemFilterByName(MyFavouritesFilter.NAME)).thenReturn(
      new MeasureFilterDto().setId(100L)
    );
    when(dao.findSystemFilterByName(ProjectFilter.NAME)).thenReturn(
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
    when(dao.findSystemFilterByName(MyFavouritesFilter.NAME)).thenReturn(null);
    when(dao.findSystemFilterByName(ProjectFilter.NAME)).thenReturn(null);

    Dashboard dashboard = template.createDashboard();
    List<Widget> firstColumn = dashboard.getWidgetsOfColumn(1);
    assertThat(firstColumn).hasSize(1);
    assertThat(firstColumn.get(0).getId()).isEqualTo(WelcomeWidget.ID);

    List<Widget> secondColumn = dashboard.getWidgetsOfColumn(2);
    assertThat(secondColumn).isEmpty();
  }
}
