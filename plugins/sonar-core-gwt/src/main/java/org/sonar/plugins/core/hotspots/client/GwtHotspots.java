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
package org.sonar.plugins.core.hotspots.client;

import com.google.gwt.gen2.table.override.client.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Page;
import org.sonar.plugins.core.hotspots.client.widget.MetricHotspot;
import org.sonar.plugins.core.hotspots.client.widget.MostBadlyDesignedFiles;
import org.sonar.plugins.core.hotspots.client.widget.MostViolatedResources;
import org.sonar.plugins.core.hotspots.client.widget.MostViolatedRules;
import org.sonar.wsclient.services.Resource;

public class GwtHotspots extends Page {
  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    Grid grid = new Grid(1, 2);
    grid.setStylePrimaryName("gwt-Hotspots");
    loadHotspots(grid, resource);
    return grid;
  }


  private void loadHotspots(Grid grid, Resource resource) {
    VerticalPanel column1 = new VerticalPanel();
    column1.setStyleName("hotspotcol");
    VerticalPanel column2 = new VerticalPanel();
    column2.setStyleName("hotspotcol");

    column1.add(new MostViolatedRules(resource));
    column1.add(new MetricHotspot(resource, Metrics.TEST_EXECUTION_TIME, I18nConstants.INSTANCE.titleLongestTests()));
    column1.add(new MetricHotspot(resource, Metrics.COMPLEXITY, I18nConstants.INSTANCE.titleMostComplexResources()));
    column1.add(new MetricHotspot(resource, Metrics.DUPLICATED_LINES, I18nConstants.INSTANCE.titleMostDuplicatedResources()));
    column1.add(new MostBadlyDesignedFiles(resource));

    column2.add(new MostViolatedResources(resource));
    column2.add(new MetricHotspot(resource, Metrics.UNCOVERED_LINES, I18nConstants.INSTANCE.titleLessTested()));
    column2.add(new MetricHotspot(resource, Metrics.FUNCTION_COMPLEXITY, I18nConstants.INSTANCE.titleMostComplexMethods()));
    column2.add(new MetricHotspot(resource, Metrics.PUBLIC_UNDOCUMENTED_API, I18nConstants.INSTANCE.titleMostUndocumentedAPI()));

    grid.setWidget(0, 0, column1);
    grid.setWidget(0, 1, column2);
  }

}
