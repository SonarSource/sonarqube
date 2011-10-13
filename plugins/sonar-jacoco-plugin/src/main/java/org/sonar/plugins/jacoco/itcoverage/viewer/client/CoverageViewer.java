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
package org.sonar.plugins.jacoco.itcoverage.viewer.client;

import org.sonar.gwt.ui.Page;
import org.sonar.gwt.ui.ViewerHeader;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Copied from org.sonar.plugins.core.coverageviewer.client.CoverageViewer
 */
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
      super(resource, new String[] { Metrics.IT_COVERAGE, Metrics.IT_LINE_COVERAGE, Metrics.IT_UNCOVERED_LINES, Metrics.IT_BRANCH_COVERAGE, Metrics.IT_UNCOVERED_CONDITIONS });
    }

    @Override
    protected void display(FlowPanel header, Resource resource) {
      HorizontalPanel panel = new HorizontalPanel();
      header.add(panel);

      Measure measure = resource.getMeasure(Metrics.IT_COVERAGE);
      if (measure == null) {
        addBigCell(panel, "-");
      } else {
        addBigCell(panel, measure.getFormattedValue());
      }

      addCell(panel, resource.getMeasure(Metrics.IT_LINE_COVERAGE));
      addCell(panel, resource.getMeasure(Metrics.IT_UNCOVERED_LINES));
      addCell(panel, resource.getMeasure(Metrics.IT_LINES_TO_COVER));
      addCell(panel, resource.getMeasure(Metrics.IT_BRANCH_COVERAGE));
      addCell(panel, resource.getMeasure(Metrics.IT_UNCOVERED_CONDITIONS));
      addCell(panel, resource.getMeasure(Metrics.IT_CONDITIONS_TO_COVER));
    }
  }
}
