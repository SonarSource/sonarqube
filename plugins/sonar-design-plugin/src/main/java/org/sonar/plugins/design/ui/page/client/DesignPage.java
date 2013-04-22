/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.design.ui.page.client;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.Configuration;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public class DesignPage extends Page {

  public static final String GWT_ID = "org.sonar.plugins.design.ui.page.DesignPage";
  private Dsm matrix;

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    String dependencyId = Configuration.getRequestParameter("depId");
    if (dependencyId != null) {
      DependencyInfo.getInstance().setPopupMode(true).show(dependencyId);
      return DependencyInfo.getInstance();
    }
    VerticalPanel layout = new VerticalPanel();
    layout.setWidth("100%");
    Panel hPanel = new VerticalPanel();
    matrix = new Dsm();
    hPanel.add(matrix);
    hPanel.add(DependencyInfo.getInstance());
    layout.add(hPanel);
    loadMatrix(resource);
    return layout;
  }

  private void loadMatrix(Resource resource) {
    Sonar.getInstance().find(ResourceQuery.createForMetrics(resource.getId().toString(), "dsm"), new AbstractCallback<Resource>() {

      @Override
      protected void doOnResponse(Resource resource) {
        if (resource == null || resource.getMeasure(Metrics.DEPENDENCY_MATRIX) == null) {
          matrix.displayNoData();
          
        } else {
          Measure dsm = resource.getMeasure(Metrics.DEPENDENCY_MATRIX);
          matrix.display(DsmData.parse(dsm.getData()));
        }
      }
    });
  }
}
