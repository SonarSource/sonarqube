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
package org.sonar.plugins.core.duplicationsviewer.client;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Page;
import org.sonar.gwt.ui.ViewerHeader;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

public class DuplicationsViewer extends Page {

  public static final String GWT_ID = "org.sonar.plugins.core.duplicationsviewer.DuplicationsViewer";

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    FlowPanel panel = new FlowPanel();
    panel.setWidth("100%");
    panel.add(new DuplicationsHeader(resource));
    panel.add(new DuplicationsPanel(resource));
    return panel;
  }

  private static class DuplicationsHeader extends ViewerHeader {
    public DuplicationsHeader(Resource resource) {
      super(resource, new String[]{Metrics.DUPLICATED_LINES_DENSITY, Metrics.LINES, Metrics.DUPLICATED_LINES, Metrics.DUPLICATED_BLOCKS});
    }

    @Override
    protected void display(FlowPanel header, Resource resource) {
      Panel panel = new HorizontalPanel();
      header.add(panel);

      Measure measure = resource.getMeasure(Metrics.DUPLICATED_LINES_DENSITY);
      if (measure == null) {
        addBigCell(panel, "0");
      } else {
        addBigCell(panel, measure.getFormattedValue());
      }

      Dictionary l10n = Dictionary.getDictionary("l10n");
      addCell(panel, getDefaultMeasure(resource, Metrics.LINES, l10n.get("dupl.lines")));
      addCell(panel, getDefaultMeasure(resource, Metrics.DUPLICATED_LINES, l10n.get("dupl.duplicated_lines")));
      addCell(panel, getDefaultMeasure(resource, Metrics.DUPLICATED_BLOCKS, l10n.get("dupl.duplicated_blocks")));
    }

    private Measure getDefaultMeasure(Resource resource, String metric, String label) {
      Measure measure = resource.getMeasure(metric);
      if (measure == null || measure.getValue() == null) {
        measure = new Measure();
        measure.setMetricName(label);
        measure.setValue(0.0);
        measure.setFormattedValue("0");
      }
      return measure;
    }
  }

}
