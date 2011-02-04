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
package org.sonar.plugins.core.testdetailsviewer.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Page;
import org.sonar.gwt.ui.ViewerHeader;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

public class TestsViewer extends Page {

  public static final String GWT_ID = "org.sonar.plugins.core.testdetailsviewer.TestsViewer";

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    FlowPanel flowPanel = new FlowPanel();
    flowPanel.add(new UnitTestsHeader(resource));
    flowPanel.add(new TestsPanel(resource));
    return flowPanel;
  }

  private static class UnitTestsHeader extends ViewerHeader {
    public UnitTestsHeader(Resource resource) {

      super(resource, new String[]{
          Metrics.TEST_ERRORS,
          Metrics.TEST_FAILURES,
          Metrics.TEST_SUCCESS_DENSITY,
          Metrics.TESTS,
          Metrics.SKIPPED_TESTS,
          Metrics.TEST_EXECUTION_TIME}
      );
    }

    @Override
    protected void display(FlowPanel header, Resource resource) {
      HorizontalPanel panel = new HorizontalPanel();
      header.add(panel);

      Measure measure = resource.getMeasure(Metrics.TEST_SUCCESS_DENSITY);
      if (measure == null) {
        addBigCell(panel, "100%"); // best value
      } else {
        addBigCell(panel, measure.getFormattedValue());
      }

      String skippedHtml = "";
      Measure skipped = resource.getMeasure(Metrics.SKIPPED_TESTS);
      if (skipped != null && skipped.getValue() > 0.0) {
        skippedHtml += " (+" + skipped.getFormattedValue() + " skipped)";
      }
      addCell(panel,
          "Tests: ",
          resource.getMeasureFormattedValue(Metrics.TESTS, "-") + skippedHtml);

      addCell(panel,
          "Failures/Errors: ",
          resource.getMeasureFormattedValue(Metrics.TEST_FAILURES, "0") + "/" + resource.getMeasureFormattedValue(Metrics.TEST_ERRORS, "0"));

      addCell(panel,
          "Duration: ",
          resource.getMeasureFormattedValue(Metrics.TEST_EXECUTION_TIME, "-"));
    }
  }

}
