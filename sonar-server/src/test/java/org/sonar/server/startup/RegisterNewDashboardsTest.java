/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;
import org.sonar.persistence.dashboard.*;
import org.sonar.persistence.template.LoadedTemplateDao;
import org.sonar.persistence.template.LoadedTemplateDto;

import java.util.ArrayList;
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

  private RegisterNewDashboards registerNewDashboards;
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

    registerNewDashboards = new RegisterNewDashboards(new DashboardTemplate[]{fakeDashboardTemplate}, dashboardDao,
      activeDashboardDao, loadedTemplateDao);
  }

  @Test
  public void testStart() throws Exception {
    registerNewDashboards.start();
    verify(dashboardDao).insert(any(org.sonar.persistence.dashboard.DashboardDto.class));
    verify(loadedTemplateDao).insert(any(LoadedTemplateDto.class));
    verify(activeDashboardDao).insert(any(ActiveDashboardDto.class));
  }

  @Test
  public void testShouldNotBeRegistered() throws Exception {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.DASHBOARD_TYPE, "fake-dashboard")).thenReturn(1);
    assertThat(registerNewDashboards.shouldRegister(fakeDashboardTemplate.createDashboard()), is(false));
  }

  @Test
  public void testShouldBeLoaded() throws Exception {
    assertThat(registerNewDashboards.shouldRegister(fakeDashboardTemplate.createDashboard()), is(true));
  }

  @Test
  public void shouldLoadDasboard() throws Exception {
    DashboardDto dashboardDto = registerNewDashboards.registerDashboard(fakeDashboardTemplate.createDashboard());
    assertNotNull(dashboardDto);
    verify(dashboardDao).insert(dashboardDto);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("fake-dashboard", LoadedTemplateDto.DASHBOARD_TYPE)));
  }

  @Test
  public void shouldCreateDtoFromExtension() {
    DashboardDto dto = registerNewDashboards.createDtoFromExtension(fakeDashboardTemplate.createDashboard());
    assertThat(dto.getUserId(), is(nullValue()));
    assertThat(dto.getKey(), is("fake-dashboard"));
    assertThat(dto.getDescription(), nullValue());
    assertThat(dto.getColumnLayout(), is("30%-70%"));
    assertThat(dto.getShared(), is(true));
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
  public void shouldCompareDashboardForSorting() throws Exception {
    DashboardDto d1 = mock(DashboardDto.class);
    when(d1.getName()).thenReturn("Foo");
    DashboardDto d2 = mock(DashboardDto.class);
    when(d2.getName()).thenReturn("Bar");
    List<DashboardDto> dashboardDtos = Lists.newArrayList(d1, d2);
    Collections.sort(dashboardDtos, new RegisterNewDashboards.DashboardComparator());

    assertThat(dashboardDtos.get(0).getName(), is("Bar"));
  }

  @Test
  public void shouldActivateAllDashboards() throws Exception {
    DashboardDto d1 = mock(DashboardDto.class);
    when(d1.getName()).thenReturn("Foo");
    when(d1.getId()).thenReturn(14L);
    DashboardDto d2 = mock(DashboardDto.class);
    when(d2.getName()).thenReturn("Bar");
    when(d2.getId()).thenReturn(16L);
    ArrayList<DashboardDto> loadedDashboards = Lists.newArrayList(d1, d2);

    when(activeDashboardDao.selectMaxOrderIndexForNullUser()).thenReturn(4);

    registerNewDashboards.activateDashboards(loadedDashboards, null);

    ActiveDashboardDto ad1 = new ActiveDashboardDto();
    ad1.setDashboardId(16L);
    ad1.setOrderIndex(5);
    verify(activeDashboardDao).insert(eq(ad1));
    ActiveDashboardDto ad2 = new ActiveDashboardDto();
    ad2.setDashboardId(14L);
    ad2.setOrderIndex(6);
    verify(activeDashboardDao).insert(eq(ad2));
  }

  @Test
  public void shouldActivateDefaultDashboard() throws Exception {
    DashboardDto defaultDashboard = mock(DashboardDto.class);
    when(defaultDashboard.getName()).thenReturn(RegisterNewDashboards.DEFAULT_DASHBOARD_ID);
    when(defaultDashboard.getId()).thenReturn(1L);
    DashboardDto d1 = mock(DashboardDto.class);
    when(d1.getName()).thenReturn("Bar");
    when(d1.getId()).thenReturn(16L);
    List<DashboardDto> loadedDashboards = Lists.newArrayList(d1);

    registerNewDashboards.activateDashboards(loadedDashboards, defaultDashboard);

    ActiveDashboardDto ad1 = new ActiveDashboardDto();
    ad1.setDashboardId(1L);
    ad1.setOrderIndex(1);
    verify(activeDashboardDao).insert(eq(ad1));
    ActiveDashboardDto ad2 = new ActiveDashboardDto();
    ad2.setDashboardId(16L);
    ad2.setOrderIndex(2);
    verify(activeDashboardDao).insert(eq(ad2));
  }

  public class FakeDashboard extends DashboardTemplate {

    @Override
    public Dashboard createDashboard() {
      Dashboard dashboard = Dashboard.create("fake-dashboard", "Fake");
      dashboard.setLayout(DashboardLayout.TWO_COLUMNS_30_70);
      Dashboard.Widget widget = dashboard.addWidget("fake-widget", 1);
      widget.setProperty("fake-property", "fake_metric");
      return dashboard;
    }
  }

}
