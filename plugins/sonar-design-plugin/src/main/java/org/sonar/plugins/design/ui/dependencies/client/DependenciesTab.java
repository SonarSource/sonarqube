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
package org.sonar.plugins.design.ui.dependencies.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.Loading;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Dependency;
import org.sonar.wsclient.services.DependencyQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

public class DependenciesTab extends Page {
  public static final String GWT_ID = "org.sonar.plugins.design.ui.dependencies.DependenciesTab";

  private FlowPanel panel = null;
  private DependenciesTable dependenciesTable = null;
  private Loading loading;

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    prepare();
    Data data = new Data(resource.getId());
    loadMeasures(data);
    loadDependencies(data);
    return panel;
  }

  private void prepare() {
    if (panel == null) {
      panel = new FlowPanel();
      panel.getElement().setId("deps");
      loading = new Loading();
      dependenciesTable = new DependenciesTable();
      panel.setWidth("100%");
    }
    panel.clear();
    panel.add(loading);
  }

  private void loadMeasures(final Data data) {
    ResourceQuery query = new ResourceQuery(data.getResourceId());
    query.setMetrics(Metrics.EFFERENT_COUPLINGS, Metrics.AFFERENT_COUPLINGS);
    query.setVerbose(true);
    Sonar.getInstance().find(query, new AbstractCallback<org.sonar.wsclient.services.Resource>() {
      @Override
      protected void doOnResponse(org.sonar.wsclient.services.Resource resource) {
        data.setMeasures(resource);
        displayMeasures(data);
      }
    });
  }

  private void loadDependencies(final Data data) {
    DependencyQuery query = DependencyQuery.createForResource(data.getResourceId());
    Sonar.getInstance().findAll(query, new AbstractListCallback<Dependency>() {

      @Override
      protected void doOnResponse(List<Dependency> dependencies) {
        data.setDependencies(dependencies);
        displayMeasures(data);
      }
    });
  }


  private void displayMeasures(Data data) {
    if (data.isLoaded()) {
      panel.clear();
      dependenciesTable.display(data);
      panel.add(dependenciesTable);
    }
  }
}
