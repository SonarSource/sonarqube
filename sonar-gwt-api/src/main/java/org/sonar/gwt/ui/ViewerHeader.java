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
package org.sonar.gwt.ui;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public abstract class ViewerHeader extends Composite {
  private String[] metrics;
  private FlowPanel header;

  public ViewerHeader(Resource resource, String[] metrics) {
    this.metrics = metrics;
    header = new FlowPanel();
    header.setStyleName("gwt-ViewerHeader");
    initWidget(header);
    loadMeasures(resource);
  }

  public String[] getMetrics() {
    return metrics;
  }

  private void loadMeasures(Resource resource) {
    ResourceQuery query = ResourceQuery.createForMetrics(resource.getKey(), metrics).setVerbose(true);
    Sonar.getInstance().find(query, new AbstractCallback<Resource>() {

      @Override
      protected void doOnResponse(Resource resource) {
        display(header, resource);
      }
    });
  }

  protected abstract void display(FlowPanel header, Resource resource);

  protected static class MeasureLabel {
    private String metricName;
    private String value;

    public MeasureLabel(Measure measure) {
      if (measure != null) {
        this.metricName = measure.getMetricName();
        this.value = measure.getFormattedValue();
      }
    }

    public MeasureLabel(Measure measure, String metricName, String defaultValue) {
      this.metricName = metricName;
      if (measure != null) {
        this.value = measure.getFormattedValue();
      } else {
        this.value = defaultValue;
      }
    }

    public String getMetricName() {
      return metricName;
    }

    public String getValue() {
      return value;
    }

    public boolean hasValue() {
      return value != null;
    }
  }

  protected void addCell(Panel panel, Measure... measures) {
    if (measures != null) {
      String names = "";
      String values = "";
      boolean first = true;
      for (Measure measure : measures) {
        if (measure != null && measure.getFormattedValue() != null) {
          if (!first) {
            names += "<br/>";
            values += "<br/>";
          }
          names += "<b>" + measure.getMetricName() + "</b>: ";
          values += measure.getFormattedValue();
          first = false;
        }
      }

      if (!first) {
        HTML html = new HTML(names);
        html.setStyleName("metric");
        panel.add(html);

        html = new HTML(values);
        html.setStyleName("value");
        panel.add(html);
      }
    }
  }

  protected void addCell(Panel panel, String metric, String value) {
    HTML html = new HTML(metric);
    html.setStyleName("metric");
    panel.add(html);

    html = new HTML(value);
    html.setStyleName("value");
    panel.add(html);
  }

  protected void addBigCell(Panel panel, String html) {
    HTML htmlDiv = new HTML(html);
    htmlDiv.setStyleName("big");
    panel.add(htmlDiv);
  }
}
