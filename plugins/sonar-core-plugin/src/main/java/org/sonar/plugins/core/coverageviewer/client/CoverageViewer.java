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
package org.sonar.plugins.core.coverageviewer.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Page;
import org.sonar.gwt.ui.ViewerHeader;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

public class CoverageViewer extends Page {
  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    FlowPanel panel = new FlowPanel();
    panel.setWidth("100%");
    panel.add(new CoverageHeader(resource));
    panel.add(new CoveragePanel(resource));
    return panel;
  }

  private static class CoverageHeader extends ViewerHeader {
    public CoverageHeader(Resource resource) {
      super(resource, new String[]{Metrics.COVERAGE, Metrics.LINE_COVERAGE, Metrics.UNCOVERED_LINES, Metrics.BRANCH_COVERAGE, Metrics.UNCOVERED_CONDITIONS});
    }

    @Override
    protected void display(FlowPanel header, Resource resource) {
      HorizontalPanel panel = new HorizontalPanel();
      header.add(panel);

      Measure measure = resource.getMeasure(Metrics.COVERAGE);
      if (measure == null) {
        addBigCell(panel, "-");
      } else {
        addBigCell(panel, measure.getFormattedValue());
      }

      addCell(panel, resource.getMeasure(Metrics.LINE_COVERAGE));
      addCell(panel, resource.getMeasure(Metrics.UNCOVERED_LINES));
      addCell(panel, resource.getMeasure(Metrics.BRANCH_COVERAGE));
      addCell(panel, resource.getMeasure(Metrics.UNCOVERED_CONDITIONS));
    }
  }
}