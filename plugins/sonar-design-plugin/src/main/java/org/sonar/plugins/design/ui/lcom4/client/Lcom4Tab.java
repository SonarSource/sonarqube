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
package org.sonar.plugins.design.ui.lcom4.client;

import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.ui.Icons;
import org.sonar.gwt.ui.Loading;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public class Lcom4Tab extends Page {
  public static final String GWT_ID = "org.sonar.plugins.design.ui.lcom4.Lcom4Tab";

  private VerticalPanel panel;

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    panel = new VerticalPanel();
    panel.setWidth("100%");
    loadData(resource);
    return panel;
  }

  private void loadData(Resource resource) {
    panel.add(new Loading());
    ResourceQuery query = ResourceQuery.createForMetrics(resource.getId().toString(), "lcom4", "lcom4_blocks");
    Sonar.getInstance().find(query, new AbstractCallback<Resource>() {
      @Override
      protected void doOnResponse(Resource resource) {
        panel.clear();
        panel.add(new Header(resource));
        if (resource != null && resource.getMeasure("lcom4_blocks") != null) {
          String json = resource.getMeasure("lcom4_blocks").getData();
          Data.Blocks blocks = Data.parse(json);

          Grid grid = new Grid(blocks.size(), 2);
          grid.setStyleName("lcom4blocks");
          grid.getColumnFormatter().setStyleName(0, "index");
          
          for (int indexBlock = 0; indexBlock < blocks.size(); indexBlock++) {
            Data.Block block = blocks.get(indexBlock);
            grid.setHTML(indexBlock, 0, "<div class='index'>" + (indexBlock + 1) + "</div>");

            VerticalPanel blockPanel = new VerticalPanel();
            blockPanel.setStyleName("lcom4block");

            for (int indexEntity = 0; indexEntity < block.size(); indexEntity++) {
              Data.Entity entity = block.get(indexEntity);
              HTML row = new HTML(Icons.forQualifier(entity.getQualifier()).getHTML() + " " + entity.getName());
              row.setStyleName("lcom4row");
              blockPanel.add(row);
            }
            grid.setWidget(indexBlock, 1, blockPanel);

          }
          panel.add(grid);
        }
      }
    });
  }

  private static class Header extends Composite {
    private FlowPanel header;

    public Header(Resource resource) {
      header = new FlowPanel();
      header.setStyleName("gwt-ViewerHeader");

      HorizontalPanel panel = new HorizontalPanel();
      HTML html = new HTML("Lack of Cohesion of Methods: ");
      html.setStyleName("metric");
      panel.add(html);

      if (resource != null) {
        html = new HTML(resource.getMeasureFormattedValue("lcom4", "-"));
        html.setStyleName("value");
        panel.add(html);
      }

      header.add(panel);
      initWidget(header);
    }
  }
}
