/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.core.clouds.client;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.*;
import org.sonar.api.web.gwt.client.AbstractPage;
import org.sonar.api.web.gwt.client.ResourceDictionary;
import org.sonar.api.web.gwt.client.webservices.*;
import org.sonar.api.web.gwt.client.webservices.WSMetrics.Metric;
import org.sonar.api.web.gwt.client.webservices.WSMetrics.MetricsList;
import org.sonar.api.web.gwt.client.widgets.LoadingLabel;
import org.sonar.plugins.core.clouds.client.widget.ClassCloudsWidget;
import org.sonar.plugins.core.clouds.client.widget.TabWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GwtClouds extends AbstractPage {

  public static final String GWT_ID = "org.sonar.plugins.core.clouds.GwtClouds";

  private Panel cloudsPanel;
  private ListBox metricsListBox;
  private Label sizeAndColorLabel;
  private TabWidget sizeTabs;
  private Resources resources;

  private final List<SizeMetric> SIZE_METRICS = Arrays.asList(
      new SizeMetric("Quick Wins", WSMetrics.NCLOC),
      new SizeMetric("Top risk", WSMetrics.FUNCTION_COMPLEXITY));

  private final List<Metric> COLOR_METRICS = Arrays.asList(WSMetrics.COVERAGE, WSMetrics.VIOLATIONS_DENSITY);

  public void onModuleLoad() {
    cloudsPanel = new FlowPanel();
    displayView(cloudsPanel);
    loadClouds();
  }

  protected void loadClouds() {
    String projectKey = ResourceDictionary.getResourceKey();
    final List<Metric> metricsToGet = new ArrayList<Metric>();
    for (SizeMetric size : SIZE_METRICS) {
      metricsToGet.add(size.getSizeMetric());
    }
    for (Metric color : COLOR_METRICS) {
      metricsToGet.add(color);
    }
    if (projectKey != null) {
      cloudsPanel.add(new LoadingLabel());

      Query<Resources> resourcesQuery = ResourcesQuery.get(projectKey).setDepth(-1).setScopes(Resource.SCOPE_ENTITY).setMetrics(metricsToGet);
      QueryCallBack<Resources> resourcesCb = new BaseQueryCallback<Resources>() {
        public void onResponse(Resources response, JavaScriptObject jsonRawResponse) {
          resources = response;
        }
      };
      Query<MetricsList> metrics = MetricsQuery.get().setUserManaged(false);
      QueryCallBack<MetricsList> metricsCb = new BaseQueryCallback<MetricsList>() {
        public void onResponse(MetricsList response, JavaScriptObject jsonRawResponse) {
          // nothing to do WSMetrics.getUpdateMetricsFromServer will update the metrics labels
        }
      };
      metricsCb = WSMetrics.getUpdateMetricsFromServer(metricsCb);

      QueryCallBack<VoidResponse> updateCloudsCb = new BaseQueryCallback<VoidResponse>() {
        public void onResponse(VoidResponse response, JavaScriptObject jsonRawResponse) {
          updateClouds(resources);
        }
      };

      SequentialQueries.get().add(resourcesQuery, resourcesCb).add(metrics, metricsCb).execute(updateCloudsCb);
    }
  }

  private void updateClouds(Resources resources) {
    cloudsPanel.clear();
    Panel metricSelectPanel = getMetricColorSelectBox(resources);
    sizeTabs = new TabWidget(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        renderClassCloudsForCurrentMetric();
      }
    });
    for (SizeMetric size : SIZE_METRICS) {
      ClassCloudsWidget classCloudsTab = new ClassCloudsWidget(resources.getResources(), size.getSizeMetric());
      sizeTabs.addTab(classCloudsTab, size.getTabName(), size.getTabNameId());
    }

    cloudsPanel.add(metricSelectPanel);
    cloudsPanel.add(sizeTabs);
  }

  private Panel getMetricColorSelectBox(Resources resources) {
    HTMLPanel metricSelectPanel = new HTMLPanel("<div id='select_metric' class='metricSelectBox small'> </div>");
    sizeAndColorLabel = new InlineLabel();
    sizeAndColorLabel.setStyleName("labelText gray");
    metricSelectPanel.add(sizeAndColorLabel, "select_metric");
    metricsListBox = new ListBox(false);
    for (Metric color : COLOR_METRICS) {
      if (resources.onceContainsMeasure(color)) {
        metricsListBox.addItem(color.getName(), color.getKey());
      }
    }
    metricSelectPanel.add(metricsListBox, "select_metric");

    metricsListBox.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        renderClassCloudsForCurrentMetric();
      }
    });
    return metricSelectPanel;
  }

  private void generateSizeAndColorLabel() {
    sizeAndColorLabel.setText("Size : " + getCurrentSizeMetric().getName() + ", color : ");
  }

  private void renderClassCloudsForCurrentMetric() {
    Widget widget = sizeTabs.getSelectedWidget();
    if (widget instanceof ClassCloudsWidget) {
      Metric current = getCurrentColorMetric();
      ClassCloudsWidget classCloudsWidget = (ClassCloudsWidget) widget;
      classCloudsWidget.generateCloud(current);
      generateSizeAndColorLabel();
    }
  }

  private Metric getCurrentColorMetric() {
    String metricKey = metricsListBox.getValue(metricsListBox.getSelectedIndex());
    for (Metric color : COLOR_METRICS) {
      if (color.getKey().equals(metricKey)) {
        return color;
      }
    }
    throw new JavaScriptException("Unable to find metric " + metricKey);
  }

  private Metric getCurrentSizeMetric() {
    String selectedTabId = sizeTabs.getSelectedTabId();
    for (SizeMetric size : SIZE_METRICS) {
      if (size.getTabNameId().equals(selectedTabId)) {
        return size.sizeMetric;
      }
    }
    throw new JavaScriptException("Unable to find metric for tab " + selectedTabId);
  }


  private class SizeMetric {

    private String tabName;
    private Metric sizeMetric;

    public SizeMetric(String tabName, Metric sizeMetric) {
      super();
      this.tabName = tabName;
      this.sizeMetric = sizeMetric;
    }

    public String getTabName() {
      return tabName;
    }

    public Metric getSizeMetric() {
      return sizeMetric;
    }

    public String getTabNameId() {
      return tabName.toLowerCase().replace(' ', '_');
    }
  }

}