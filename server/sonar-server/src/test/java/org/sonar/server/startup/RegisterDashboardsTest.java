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
package org.sonar.server.startup;

import com.google.common.collect.Iterables;
import org.hamcrest.BaseMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;
import org.sonar.core.dashboard.*;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegisterDashboardsTest {
  private RegisterDashboards task;
  private DashboardDao dashboardDao;
  private ActiveDashboardDao activeDashboardDao;
  private LoadedTemplateDao loadedTemplateDao;
  private DashboardTemplate fakeDashboardTemplate;

  @Before
  public void init() {
    dashboardDao = mock(DashboardDao.class);
    activeDashboardDao = mock(ActiveDashboardDao.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);
    fakeDashboardTemplate = mock(DashboardTemplate.class);

    task = new RegisterDashboards(new DashboardTemplate[]{fakeDashboardTemplate}, dashboardDao,
      activeDashboardDao, loadedTemplateDao, null);
  }

  @Test
  public void testStart() {
    when(fakeDashboardTemplate.createDashboard()).thenReturn(Dashboard.create());

    task.start();

    verify(dashboardDao).insert(any(DashboardDto.class));
    verify(loadedTemplateDao).insert(any(LoadedTemplateDto.class));
    verify(activeDashboardDao).insert(any(ActiveDashboardDto.class));

    task.stop();
  }

  @Test
  public void shouldNotRegister() {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.DASHBOARD_TYPE, "Fake")).thenReturn(1);

    assertThat(task.shouldRegister("Fake")).isFalse();
  }

  @Test
  public void shouldRegisterDashboard() {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.DASHBOARD_TYPE, "Fake")).thenReturn(0);

    assertThat(task.shouldRegister("Fake")).isTrue();
  }

  @Test
  public void should_register_and_activate_dashboard() {
    when(fakeDashboardTemplate.createDashboard()).thenReturn(Dashboard.create());

    DashboardDto dashboardDto = task.register("Fake", fakeDashboardTemplate.createDashboard());

    verify(dashboardDao).insert(dashboardDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("Fake", LoadedTemplateDto.DASHBOARD_TYPE)));
  }

  @Test
  public void should_activate_dashboard() {
    Dashboard dashboard = Dashboard.create();
    when(fakeDashboardTemplate.createDashboard()).thenReturn(dashboard);

    task.start();

    verify(activeDashboardDao).insert(any(ActiveDashboardDto.class));
  }

  @Test
  public void should_disable_activation() {
    Dashboard dashboard = Dashboard.create();
    dashboard.setActivated(false);
    when(fakeDashboardTemplate.createDashboard()).thenReturn(dashboard);

    task.start();

    verify(activeDashboardDao, never()).insert(any(ActiveDashboardDto.class));
  }

  @Test
  public void shouldCreateDtoFromExtension() {
    Dashboard dashboard = Dashboard.create()
      .setGlobal(true)
      .setLayout(DashboardLayout.TWO_COLUMNS_30_70);
    Dashboard.Widget widget = dashboard.addWidget("fake-widget", 1);
    widget.setProperty("fake-property", "fake_metric");
    when(fakeDashboardTemplate.createDashboard()).thenReturn(dashboard);

    DashboardDto dto = task.createDtoFromExtension("Fake", fakeDashboardTemplate.createDashboard());
    assertThat(dto.getUserId()).isNull();
    assertThat(dto.getName()).isEqualTo("Fake");
    assertThat(dto.getDescription()).isNull();
    assertThat(dto.getColumnLayout()).isEqualTo("30%-70%");
    assertThat(dto.getShared()).isTrue();
    assertThat(dto.getGlobal()).isTrue();
    assertThat(dto.getCreatedAt()).isNotNull();
    assertThat(dto.getUpdatedAt()).isNotNull();

    WidgetDto widgetDto = Iterables.getOnlyElement(dto.getWidgets());
    assertThat(widgetDto.getWidgetKey()).isEqualTo("fake-widget");
    assertThat(widgetDto.getDescription()).isNull();
    assertThat(widgetDto.getColumnIndex()).isEqualTo(1);
    assertThat(widgetDto.getRowIndex()).isEqualTo(1);
    assertThat(widgetDto.getConfigured()).isTrue();
    assertThat(widgetDto.getCreatedAt()).isNotNull();
    assertThat(widgetDto.getUpdatedAt()).isNotNull();

    Collection<WidgetPropertyDto> props = widgetDto.getWidgetProperties();
    assertThat(props).hasSize(1);
    WidgetPropertyDto prop = Iterables.getFirst(props, null);
    assertThat(prop.getPropertyKey()).isEqualTo("fake-property");
    assertThat(prop.getTextValue()).isEqualTo("fake_metric");
  }

  @Test
  public void defaultDashboardShouldBeTheFirstActivatedDashboard() {
    DashboardDto defaultDashboard = new DashboardDto().setId(10L).setName(RegisterDashboards.DEFAULT_DASHBOARD_NAME);
    DashboardDto second = new DashboardDto().setId(11L).setName("Bar");
    DashboardDto third = new DashboardDto().setId(12L).setName("Foo");
    List<DashboardDto> dashboards = Arrays.asList(third, defaultDashboard, second);

    task.activate(dashboards);

    verify(activeDashboardDao).insert(argThat(matchActiveDashboardDto(10L, 1)));
    verify(activeDashboardDao).insert(argThat(matchActiveDashboardDto(11L, 2)));
    verify(activeDashboardDao).insert(argThat(matchActiveDashboardDto(12L, 3)));
  }

  private BaseMatcher<ActiveDashboardDto> matchActiveDashboardDto(final long dashboardId, final int orderId) {
    return new ArgumentMatcher<ActiveDashboardDto>() {
      @Override
      public boolean matches(Object o) {
        ActiveDashboardDto dto = (ActiveDashboardDto) o;
        return dto.getDashboardId() == dashboardId && dto.getOrderIndex() == orderId;
      }
    };
  }
}
