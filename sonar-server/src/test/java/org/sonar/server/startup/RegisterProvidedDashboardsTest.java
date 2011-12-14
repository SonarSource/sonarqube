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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.web.dashboard.Dashboard;
import org.sonar.api.web.dashboard.DashboardTemplate;
import org.sonar.core.i18n.I18nManager;
import org.sonar.persistence.dashboard.ActiveDashboardDao;
import org.sonar.persistence.dashboard.ActiveDashboardDto;
import org.sonar.persistence.dashboard.DashboardDao;
import org.sonar.persistence.dashboard.WidgetDto;
import org.sonar.persistence.template.LoadedTemplateDto;
import org.sonar.persistence.template.LoadedTemplateDao;

import com.google.common.collect.Lists;

public class RegisterProvidedDashboardsTest {

  private RegisterProvidedDashboards registerProvidedDashboards;
  private DashboardDao dashboardDao;
  private ActiveDashboardDao activeDashboardDao;
  private LoadedTemplateDao loadedTemplateDao;
  private I18nManager i18nManager;
  private DashboardTemplate fakeDashboardTemplate;

  @Before
  public void init() {
    dashboardDao = mock(DashboardDao.class);
    activeDashboardDao = mock(ActiveDashboardDao.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);
    i18nManager = mock(I18nManager.class);
    when(i18nManager.message(Locale.ENGLISH, "widget.fake-widget.name", "")).thenReturn("Fake Widget");
    when(i18nManager.message(Locale.ENGLISH, "dashboard.fake-dashboard.name", "Fake")).thenReturn("Fake Dashboard");

    fakeDashboardTemplate = new FakeDashboard();

    registerProvidedDashboards = new RegisterProvidedDashboards(new DashboardTemplate[] { fakeDashboardTemplate }, dashboardDao,
        activeDashboardDao, loadedTemplateDao, i18nManager);
  }

  @Test
  public void testStart() throws Exception {
    registerProvidedDashboards.start();
    verify(dashboardDao).insert(any(org.sonar.persistence.dashboard.DashboardDto.class));
    verify(loadedTemplateDao).insert(any(LoadedTemplateDto.class));
    verify(activeDashboardDao).insert(any(ActiveDashboardDto.class));
  }

  @Test
  public void testShouldNotBeLoaded() throws Exception {
    when(loadedTemplateDao.selectByKeyAndType("fake-dashboard", LoadedTemplateDto.DASHBOARD_TYPE)).thenReturn(new LoadedTemplateDto());
    assertThat(registerProvidedDashboards.shouldBeLoaded(fakeDashboardTemplate.createDashboard()), is(false));
  }

  @Test
  public void testShouldBeLoaded() throws Exception {
    assertThat(registerProvidedDashboards.shouldBeLoaded(fakeDashboardTemplate.createDashboard()), is(true));
  }

  @Test
  public void shouldLoadDasboard() throws Exception {
    org.sonar.persistence.dashboard.DashboardDto dataModelDashboard = registerProvidedDashboards.loadDashboard(fakeDashboardTemplate
        .createDashboard());
    assertNotNull(dataModelDashboard);
    verify(dashboardDao).insert(dataModelDashboard);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplateDto("fake-dashboard", LoadedTemplateDto.DASHBOARD_TYPE)));
  }

  @Test
  public void shouldCreateDataModelFromExtension() {
    org.sonar.persistence.dashboard.DashboardDto dataModelDashboard = registerProvidedDashboards
        .createDataModelFromExtension(fakeDashboardTemplate.createDashboard());
    assertThat(dataModelDashboard.getUserId(), is(nullValue()));
    assertThat(dataModelDashboard.getKey(), is("fake-dashboard"));
    assertThat(dataModelDashboard.getName(), is("Fake Dashboard"));
    assertThat(dataModelDashboard.getDescription(), is(""));
    assertThat(dataModelDashboard.getColumnLayout(), is("30%-70%"));
    assertThat(dataModelDashboard.getShared(), is(true));
    assertNotNull(dataModelDashboard.getCreatedAt());
    assertNotNull(dataModelDashboard.getUpdatedAt());

    WidgetDto widgetDto = dataModelDashboard.getWidgets().iterator().next();
    assertThat(widgetDto.getKey(), is("fake-widget"));
    assertThat(widgetDto.getName(), is("Fake Widget"));
    assertThat(widgetDto.getDescription(), is(nullValue()));
    assertThat(widgetDto.getColumnIndex(), is(12));
    assertThat(widgetDto.getRowIndex(), is(13));
    assertThat(widgetDto.getConfigured(), is(true));
    assertNotNull(widgetDto.getCreatedAt());
    assertNotNull(widgetDto.getUpdatedAt());

    org.sonar.persistence.dashboard.WidgetPropertyDto widgetPropertyDto = widgetDto.getWidgetProperties().iterator().next();
    assertThat(widgetPropertyDto.getKey(), is("fake-property"));
    assertThat(widgetPropertyDto.getValue(), is("fake_metric"));
  }

  @Test
  public void shouldCompareDashboardForSorting() throws Exception {
    org.sonar.persistence.dashboard.DashboardDto d1 = mock(org.sonar.persistence.dashboard.DashboardDto.class);
    when(d1.getName()).thenReturn("Foo");
    org.sonar.persistence.dashboard.DashboardDto d2 = mock(org.sonar.persistence.dashboard.DashboardDto.class);
    when(d2.getName()).thenReturn("Bar");
    List<org.sonar.persistence.dashboard.DashboardDto> dashboardDtos = Lists.newArrayList(d1, d2);
    Collections.sort(dashboardDtos, new RegisterProvidedDashboards.DashboardComparator());

    assertThat(dashboardDtos.get(0).getName(), is("Bar"));
  }

  @Test
  public void shouldActivateAllDashboards() throws Exception {
    org.sonar.persistence.dashboard.DashboardDto d1 = mock(org.sonar.persistence.dashboard.DashboardDto.class);
    when(d1.getName()).thenReturn("Foo");
    when(d1.getId()).thenReturn(14L);
    org.sonar.persistence.dashboard.DashboardDto d2 = mock(org.sonar.persistence.dashboard.DashboardDto.class);
    when(d2.getName()).thenReturn("Bar");
    when(d2.getId()).thenReturn(16L);
    ArrayList<org.sonar.persistence.dashboard.DashboardDto> loadedDashboards = Lists.newArrayList(d1, d2);

    when(activeDashboardDao.selectMaxOrderIndexForNullUser()).thenReturn(4);

    registerProvidedDashboards.activateDashboards(loadedDashboards, null);

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
  public void shouldActivateMainDashboard() throws Exception {
    org.sonar.persistence.dashboard.DashboardDto mainDashboard = mock(org.sonar.persistence.dashboard.DashboardDto.class);
    when(mainDashboard.getName()).thenReturn("Main");
    when(mainDashboard.getId()).thenReturn(1L);
    org.sonar.persistence.dashboard.DashboardDto d1 = mock(org.sonar.persistence.dashboard.DashboardDto.class);
    when(d1.getName()).thenReturn("Bar");
    when(d1.getId()).thenReturn(16L);
    ArrayList<org.sonar.persistence.dashboard.DashboardDto> loadedDashboards = Lists.newArrayList(d1);

    registerProvidedDashboards.activateDashboards(loadedDashboards, mainDashboard);

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
      Dashboard dashboard = Dashboard.createDashboard("fake-dashboard", "Fake", "30%-70%");
      org.sonar.api.web.dashboard.Widget widget = dashboard.addWidget("fake-widget", 12, 13);
      widget.addProperty("fake-property", "fake_metric");
      return dashboard;
    }
  }

}
