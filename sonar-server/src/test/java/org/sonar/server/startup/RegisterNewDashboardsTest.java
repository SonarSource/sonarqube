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
package org.sonar.server.startup;

import com.google.common.collect.Lists;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;
import org.sonar.core.dashboard.*;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class RegisterNewDashboardsTest {

  private RegisterNewDashboards task;
  private DashboardDao dashboardDao;
  private ActiveDashboardDao activeDashboardDao;
  private LoadedTemplateDao loadedTemplateDao;
  private DashboardTemplate fakeDashboardTemplate;

  @Before
  public void init() {
    dashboardDao = mock(DashboardDao.class);
    activeDashboardDao = mock(ActiveDashboardDao.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);

    fakeDashboardTemplate = new FakeDashboard();

    task = new RegisterNewDashboards(new DashboardTemplate[]{fakeDashboardTemplate}, dashboardDao,
      activeDashboardDao, loadedTemplateDao);
  }

  @Test
  public void testStart() {
    task.start();
    verify(dashboardDao).insert(any(DashboardDto.class));
    verify(loadedTemplateDao).insert(any(LoadedTemplateDto.class));
    verify(activeDashboardDao).insert(any(ActiveDashboardDto.class));
  }

  @Test
  public void shouldNotRegister() {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.DASHBOARD_TYPE, "Fake")).thenReturn(1);
    assertThat(task.shouldRegister("Fake"), is(false));
  }

  @Test
  public void shouldRegisterDashboard() {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.DASHBOARD_TYPE, "Fake")).thenReturn(0);
    assertThat(task.shouldRegister("Fake"), is(true));
  }

  @Test
  public void testRegisterDashboard() {
    DashboardDto dashboardDto = task.register("Fake", fakeDashboardTemplate.createDashboard());

    assertNotNull(dashboardDto);
    verify(dashboardDao).insert(dashboardDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("fake-dashboard", LoadedTemplateDto.DASHBOARD_TYPE)));
  }

  @Test
  public void shouldCreateDtoFromExtension() {
    DashboardDto dto = task.createDtoFromExtension("Fake", fakeDashboardTemplate.createDashboard());
    assertThat(dto.getUserId(), is(nullValue()));
    assertThat(dto.getName(), is("Fake"));
    assertThat(dto.getDescription(), nullValue());
    assertThat(dto.getColumnLayout(), is("30%-70%"));
    assertThat(dto.getShared(), is(true));
    assertThat(dto.getGlobal(), is(true));
    assertNotNull(dto.getCreatedAt());
    assertNotNull(dto.getUpdatedAt());

    WidgetDto widgetDto = dto.getWidgets().iterator().next();
    assertThat(widgetDto.getKey(), is("fake-widget"));
    assertThat(widgetDto.getDescription(), is(nullValue()));
    assertThat(widgetDto.getColumnIndex(), is(1));
    assertThat(widgetDto.getRowIndex(), is(1));
    assertThat(widgetDto.getConfigured(), is(true));
    assertNotNull(widgetDto.getCreatedAt());
    assertNotNull(widgetDto.getUpdatedAt());

    WidgetPropertyDto widgetPropertyDto = widgetDto.getWidgetProperties().iterator().next();
    assertThat(widgetPropertyDto.getKey(), is("fake-property"));
    assertThat(widgetPropertyDto.getValue(), is("fake_metric"));
  }

  @Test
  public void shouldCompareDashboards() throws Exception {
    DashboardDto d1 = new DashboardDto().setName("Foo");
    DashboardDto d2 = new DashboardDto().setName("Bar");
    List<DashboardDto> dashboardDtos = Lists.newArrayList(d1, d2);
    Collections.sort(dashboardDtos, new RegisterNewDashboards.DashboardComparator());

    assertThat(dashboardDtos.get(0).getName(), is("Bar"));
  }

  @Test
  public void shouldActivateDashboards() throws Exception {
    DashboardDto d1 = new DashboardDto().setName("Foo").setId(14L);
    DashboardDto d2 = new DashboardDto().setName("Bar").setId(16L);
    List<DashboardDto> loadedDashboards = Lists.newArrayList(d1, d2);

    when(activeDashboardDao.selectMaxOrderIndexForNullUser()).thenReturn(4);

    task.activate(loadedDashboards);

    verify(activeDashboardDao).insert(argThat(matchActiveDashboard(16L, 5)));
    verify(activeDashboardDao).insert(argThat(matchActiveDashboard(14L, 6)));
  }

  @Test
  public void defaultDashboardShouldBeTheFirstActivatedDashboard() throws Exception {
    DashboardDto defaultDashboard = new DashboardDto()
      .setName(RegisterNewDashboards.DEFAULT_DASHBOARD_NAME)
      .setId(10L);
    DashboardDto other = new DashboardDto()
      .setName("Bar")
      .setId(11L);
    List<DashboardDto> dashboards = Lists.newArrayList(other, defaultDashboard);

    task.activate(dashboards);

    verify(activeDashboardDao).insert(argThat(matchActiveDashboard(10L, 1)));
    verify(activeDashboardDao).insert(argThat(matchActiveDashboard(11L, 2)));
  }

  private BaseMatcher<ActiveDashboardDto> matchActiveDashboard(final long dashboardId, final int orderId) {
    return new BaseMatcher<ActiveDashboardDto>() {
      public boolean matches(Object o) {
        ActiveDashboardDto dto = (ActiveDashboardDto) o;
        return dto.getDashboardId() == dashboardId && dto.getOrderIndex() == orderId;
      }

      public void describeTo(Description description) {
      }
    };
  }

  public class FakeDashboard extends DashboardTemplate {
    @Override
    public String getName() {
      return "Fake";
    }

    @Override
    public Dashboard createDashboard() {
      Dashboard dashboard = Dashboard.create();
      dashboard.setGlobal(true);
      dashboard.setLayout(DashboardLayout.TWO_COLUMNS_30_70);
      Dashboard.Widget widget = dashboard.addWidget("fake-widget", 1);
      widget.setProperty("fake-property", "fake_metric");
      return dashboard;
    }
  }
}
