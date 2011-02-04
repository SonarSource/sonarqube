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
package org.sonar.plugins.core.defaultsourceviewer.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.DefaultSourcePanel;
import org.sonar.gwt.ui.Page;
import org.sonar.gwt.ui.ViewerHeader;
import org.sonar.wsclient.services.Resource;

public class GwtDefaultSourceViewer extends Page {

  public static final String GWT_ID = "org.sonar.plugins.core.defaultsourceviewer.GwtDefaultSourceViewer";

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    FlowPanel panel = new FlowPanel();
    panel.setWidth("100%");
    panel.add(new SimpleHeader(resource));
    panel.add(new DefaultSourcePanel(resource));
    return panel;
  }

  private static class SimpleHeader extends ViewerHeader {
    public SimpleHeader(Resource resource) {
      super(resource, new String[]{
          Metrics.LINES,
          Metrics.NCLOC,
          Metrics.FUNCTIONS,
          Metrics.ACCESSORS,
          Metrics.PARAGRAPHS,

          Metrics.STATEMENTS,
          Metrics.COMPLEXITY,
          Metrics.FUNCTION_COMPLEXITY,
          Metrics.PARAGRAPH_COMPLEXITY,

          Metrics.COMMENT_LINES_DENSITY,
          Metrics.COMMENT_LINES,
          Metrics.COMMENTED_OUT_CODE_LINES,
          Metrics.COMMENT_BLANK_LINES,

          Metrics.PUBLIC_DOCUMENTED_API_DENSITY,
          Metrics.PUBLIC_UNDOCUMENTED_API,
          Metrics.PUBLIC_API,

          Metrics.CLASSES,
          Metrics.NUMBER_OF_CHILDREN,
          Metrics.DEPTH_IN_TREE,
          Metrics.RFC
      }
      );
    }

    @Override
    protected void display(FlowPanel header, Resource resource) {
      HorizontalPanel panel = new HorizontalPanel();
      addCell(panel,
          resource.getMeasure(Metrics.LINES),
          resource.getMeasure(Metrics.NCLOC),
          resource.getMeasure(Metrics.FUNCTIONS),
          resource.getMeasure(Metrics.ACCESSORS),
          resource.getMeasure(Metrics.PARAGRAPHS));

      addCell(panel,
          resource.getMeasure(Metrics.STATEMENTS),
          resource.getMeasure(Metrics.COMPLEXITY),
          resource.getMeasure(Metrics.FUNCTION_COMPLEXITY),
          resource.getMeasure(Metrics.PARAGRAPH_COMPLEXITY));

      addCell(panel,
          resource.getMeasure(Metrics.COMMENT_LINES_DENSITY),
          resource.getMeasure(Metrics.COMMENT_LINES),
          resource.getMeasure(Metrics.COMMENTED_OUT_CODE_LINES),
          resource.getMeasure(Metrics.COMMENT_BLANK_LINES));

      addCell(panel,
          resource.getMeasure(Metrics.PUBLIC_DOCUMENTED_API_DENSITY),
          resource.getMeasure(Metrics.PUBLIC_UNDOCUMENTED_API),
          resource.getMeasure(Metrics.PUBLIC_API));

      addCell(panel,
          resource.getMeasure(Metrics.CLASSES),
          resource.getMeasure(Metrics.NUMBER_OF_CHILDREN),
          resource.getMeasure(Metrics.DEPTH_IN_TREE),
          resource.getMeasure(Metrics.RFC));

      if (panel.getWidgetCount() > 0) {
        header.add(panel);
      }
    }
  }
}