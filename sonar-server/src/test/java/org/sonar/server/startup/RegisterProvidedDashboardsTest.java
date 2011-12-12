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
import org.sonar.api.web.AbstractDashboard;
import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayouts;
import org.sonar.api.web.DashboardWidget;
import org.sonar.api.web.DashboardWidgets;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;
import org.sonar.core.i18n.I18nManager;
import org.sonar.persistence.dao.ActiveDashboardDao;
import org.sonar.persistence.dao.DashboardDao;
import org.sonar.persistence.dao.LoadedTemplateDao;
import org.sonar.persistence.model.ActiveDashboard;
import org.sonar.persistence.model.LoadedTemplate;
import org.sonar.persistence.model.Widget;

import com.google.common.collect.Lists;

public class RegisterProvidedDashboardsTest {

  private RegisterProvidedDashboards registerProvidedDashboards;
  private DashboardDao dashboardDao;
  private ActiveDashboardDao activeDashboardDao;
  private LoadedTemplateDao loadedTemplateDao;
  private Dashboard dashboard;
  private I18nManager i18nManager;

  @Before
  public void init() {
    dashboardDao = mock(DashboardDao.class);
    activeDashboardDao = mock(ActiveDashboardDao.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);
    i18nManager = mock(I18nManager.class);
    when(i18nManager.message(Locale.ENGLISH, "widget.fake-widget.name", "")).thenReturn("Fake Widget");
    when(i18nManager.message(Locale.ENGLISH, "dashboard.fake-dashboard.name", "Fake")).thenReturn("Fake Dashboard");
    dashboard = new FakeDashboard();

    registerProvidedDashboards = new RegisterProvidedDashboards(new Dashboard[] { dashboard }, dashboardDao, activeDashboardDao,
        loadedTemplateDao, i18nManager);
  }

  @Test
  public void testShouldNotBeLoaded() throws Exception {
    when(loadedTemplateDao.selectByKeyAndType("fake-dashboard", LoadedTemplate.DASHBOARD_TYPE)).thenReturn(new LoadedTemplate());
    assertThat(registerProvidedDashboards.shouldBeLoaded(dashboard), is(false));
  }

  @Test
  public void testShouldBeLoaded() throws Exception {
    assertThat(registerProvidedDashboards.shouldBeLoaded(dashboard), is(true));
  }

  @Test
  public void shouldLoadDasboard() throws Exception {
    org.sonar.persistence.model.Dashboard dataModelDashboard = registerProvidedDashboards.loadDashboard(dashboard);
    assertNotNull(dataModelDashboard);
    verify(dashboardDao).insert(dataModelDashboard);
    verify(loadedTemplateDao).insert(eq(new LoadedTemplate("fake-dashboard", LoadedTemplate.DASHBOARD_TYPE)));
  }

  @Test
  public void test() {
    org.sonar.persistence.model.Dashboard dataModelDashboard = registerProvidedDashboards.createDataModelFromExtension(new HotspotsDashboard());
    System.out.println(dataModelDashboard);
  }

  @Test
  public void shouldCreateDataModelFromExtension() {
    org.sonar.persistence.model.Dashboard dataModelDashboard = registerProvidedDashboards.createDataModelFromExtension(dashboard);
    assertThat(dataModelDashboard.getUserId(), is(nullValue()));
    assertThat(dataModelDashboard.getKey(), is("fake-dashboard"));
    assertThat(dataModelDashboard.getName(), is("Fake Dashboard"));
    assertThat(dataModelDashboard.getDescription(), is(""));
    assertThat(dataModelDashboard.getColumnLayout(), is("30%-70%"));
    assertThat(dataModelDashboard.getShared(), is(true));
    assertNotNull(dataModelDashboard.getCreatedAt());
    assertNotNull(dataModelDashboard.getUpdatedAt());

    Widget widget = dataModelDashboard.getWidgets().iterator().next();
    assertThat(widget.getKey(), is("fake-widget"));
    assertThat(widget.getName(), is("Fake Widget"));
    assertThat(widget.getDescription(), is(nullValue()));
    assertThat(widget.getColumnIndex(), is(12));
    assertThat(widget.getRowIndex(), is(13));
    assertThat(widget.getConfigured(), is(true));
    assertNotNull(widget.getCreatedAt());
    assertNotNull(widget.getUpdatedAt());

    org.sonar.persistence.model.WidgetProperty widgetProperty = widget.getWidgetProperties().iterator().next();
    assertThat(widgetProperty.getKey(), is("fake-property"));
    assertThat(widgetProperty.getValue(), is("fake_metric"));
    assertThat(widgetProperty.getValueType(), is("METRIC"));
  }

  @Test
  public void shouldCompareDashboardForSorting() throws Exception {
    org.sonar.persistence.model.Dashboard d1 = mock(org.sonar.persistence.model.Dashboard.class);
    when(d1.getName()).thenReturn("Foo");
    org.sonar.persistence.model.Dashboard d2 = mock(org.sonar.persistence.model.Dashboard.class);
    when(d2.getName()).thenReturn("Bar");
    List<org.sonar.persistence.model.Dashboard> dashboards = Lists.newArrayList(d1, d2);
    Collections.sort(dashboards, new RegisterProvidedDashboards.DashboardComparator());

    assertThat(dashboards.get(0).getName(), is("Bar"));
  }

  @Test
  public void shouldActivateAllDashboards() throws Exception {
    org.sonar.persistence.model.Dashboard d1 = mock(org.sonar.persistence.model.Dashboard.class);
    when(d1.getName()).thenReturn("Foo");
    when(d1.getId()).thenReturn(14L);
    org.sonar.persistence.model.Dashboard d2 = mock(org.sonar.persistence.model.Dashboard.class);
    when(d2.getName()).thenReturn("Bar");
    when(d2.getId()).thenReturn(16L);
    ArrayList<org.sonar.persistence.model.Dashboard> loadedDashboards = Lists.newArrayList(d1, d2);

    when(activeDashboardDao.selectMaxOrderIndexForNullUser()).thenReturn(4);

    registerProvidedDashboards.activateDashboards(loadedDashboards, null);

    ActiveDashboard ad1 = new ActiveDashboard();
    ad1.setDashboardId(16L);
    ad1.setOrderIndex(5);
    verify(activeDashboardDao).insert(eq(ad1));
    ActiveDashboard ad2 = new ActiveDashboard();
    ad2.setDashboardId(14L);
    ad2.setOrderIndex(6);
    verify(activeDashboardDao).insert(eq(ad2));
  }

  @Test
  public void shouldActivateMainDashboard() throws Exception {
    org.sonar.persistence.model.Dashboard mainDashboard = mock(org.sonar.persistence.model.Dashboard.class);
    when(mainDashboard.getName()).thenReturn("Main");
    when(mainDashboard.getId()).thenReturn(1L);
    org.sonar.persistence.model.Dashboard d1 = mock(org.sonar.persistence.model.Dashboard.class);
    when(d1.getName()).thenReturn("Bar");
    when(d1.getId()).thenReturn(16L);
    ArrayList<org.sonar.persistence.model.Dashboard> loadedDashboards = Lists.newArrayList(d1);

    registerProvidedDashboards.activateDashboards(loadedDashboards, mainDashboard);

    ActiveDashboard ad1 = new ActiveDashboard();
    ad1.setDashboardId(1L);
    ad1.setOrderIndex(1);
    verify(activeDashboardDao).insert(eq(ad1));
    ActiveDashboard ad2 = new ActiveDashboard();
    ad2.setDashboardId(16L);
    ad2.setOrderIndex(2);
    verify(activeDashboardDao).insert(eq(ad2));
  }

  @DashboardWidgets({ @DashboardWidget(id = "fake-widget", columnIndex = 12, rowIndex = 13, properties = { @WidgetProperty(
      key = "fake-property", type = WidgetPropertyType.METRIC, defaultValue = "fake_metric") }) })
  public class FakeDashboard extends AbstractDashboard implements Dashboard {

    @Override
    public String getId() {
      return "fake-dashboard";
    }

    @Override
    public String getName() {
      return "Fake";
    }

    @Override
    public String getLayout() {
      return "30%-70%";
    }

  }
  
  @DashboardWidgets ({
    @DashboardWidget(id="hotspot_most_violated_rules", columnIndex=1, rowIndex=1),
    @DashboardWidget(id="hotspot_metric", columnIndex=1, rowIndex=2,
                      properties={
                        @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "test_execution_time"),
                        @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Longest unit tests")

    }),
    @DashboardWidget(id="hotspot_metric", columnIndex=1, rowIndex=3,
                      properties={
                        @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "complexity"),
                        @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest complexity")

    }),
    @DashboardWidget(id="hotspot_metric", columnIndex=1, rowIndex=4,
                      properties={
                        @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "duplicated_lines"),
                        @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest duplications")

    }),
    @DashboardWidget(id="hotspot_most_violated_resources", columnIndex=2, rowIndex=1),
    @DashboardWidget(id="hotspot_metric", columnIndex=2, rowIndex=2,
                      properties={
                        @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "uncovered_lines"),
                        @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest untested lines")

    }),
    @DashboardWidget(id="hotspot_metric", columnIndex=2, rowIndex=3,
                      properties={
                        @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "function_complexity"),
                        @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Highest average method complexity")

    }),
    @DashboardWidget(id="hotspot_metric", columnIndex=2, rowIndex=4,
                      properties={
                        @WidgetProperty(key = "metric", type = WidgetPropertyType.METRIC, defaultValue = "public_undocumented_api"),
                        @WidgetProperty(key = "title", type = WidgetPropertyType.STRING, defaultValue = "Most undocumented APIs")

    })
  })
  /**
   * Hotspot dashboard for Sonar
   */
  public class HotspotsDashboard extends AbstractDashboard implements Dashboard {

    @Override
    public String getId() {
      return "sonar-hotspots-dashboard";
    }

    @Override
    public String getName() {
      return "Hotspots";
    }
    
    @Override
    public String getLayout() {
      return DashboardLayouts.TWO_COLUMNS;
    }

  }
  
}
