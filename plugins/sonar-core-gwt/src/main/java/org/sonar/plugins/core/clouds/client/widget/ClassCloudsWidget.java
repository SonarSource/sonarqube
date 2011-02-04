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
package org.sonar.plugins.core.clouds.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.api.web.gwt.client.Utils;
import org.sonar.api.web.gwt.client.webservices.Measure;
import org.sonar.api.web.gwt.client.webservices.Resource;
import org.sonar.api.web.gwt.client.webservices.WSMetrics.Metric;
import org.sonar.api.web.gwt.client.widgets.LoadingLabel;
import org.sonar.plugins.core.clouds.client.Calculator;
import org.sonar.plugins.core.clouds.client.GwtClouds;
import org.sonar.plugins.core.clouds.client.model.CloudElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassCloudsWidget extends Composite {

  private Panel main;
  private Metric sizeMetric;
  private List<Resource> resources;
  private float minSizePercent = 60f;
  private float maxSizePercent = 240f;

  private Calculator sizeCalculator = new Calculator(minSizePercent, maxSizePercent);
  private Calculator colorCalculator = new Calculator(0f, 100f);

  public ClassCloudsWidget(List<Resource> resources, Metric sizeMetric) {
    this.sizeMetric = sizeMetric;
    this.main = new FlowPanel();
    this.resources = resources;
    initWidget(main);
  }

  public Metric getSizeMetric() {
    return sizeMetric;
  }

  public void generateCloud(Metric colorMetric) {
    main.clear();
    LoadingLabel loading = new LoadingLabel();
    main.add(loading);
    if (colorMetric.equals(colorMetric)) {
      List<CloudElement> cloudElements = getCloudElements(resources, colorMetric);
      createClouds(cloudElements, colorMetric);
    }
    main.remove(loading);
  }

  private List<CloudElement> getCloudElements(List<Resource> resources, Metric colorMetric) {
    List<CloudElement> tagList = new ArrayList<CloudElement>();
    for (Resource resource : resources) {
      Measure sizeMeasure = getMeasure(resource, sizeMetric);
      Measure colorMeasure = getMeasure(resource, colorMetric);

      if (sizeMeasure != null && colorMeasure != null) {
        Integer size = getMeasureValue(sizeMeasure.getValue());
        float color = colorMeasure.getValue().floatValue();
        tagList.add(new CloudElement(resource, size, color));
        sizeCalculator.updateMaxAndMin(Float.valueOf(size.toString()));
      }
    }
    Collections.sort(tagList);
    return tagList;
  }

  private Integer getMeasureValue(Double value) {
    Float floatValue = (value.floatValue() * 100.0f);
    return floatValue.intValue();
  }

  private Measure getMeasure(Resource project, Metric metricToFind) {
    return project.getMeasure(metricToFind);
  }

  private void createClouds(List<CloudElement> cloudElements, Metric colorMetric) {
    for (CloudElement tag : cloudElements) {
      HTML className = new HTML(
          "<span style=\"font-size:" + Integer.toString(sizeCalculator.getFontSizePercent(tag.getFontSize())) +
              "%; color:" + colorCalculator.getFontColor(tag.getFontColor()) + "\" >" +
              tag.getResource().getName() + "</span>\n");
      className.setStyleName("inline");

      Hyperlink link = createLink(tag, colorMetric);
      link.setHTML(className.getHTML());
      main.add(link);
    }
  }

  private Hyperlink createLink(CloudElement tag, final Metric colorMetric) {
    Hyperlink link = new Hyperlink();
    link.setStyleName("tag inline");
    String tooltip = getTooltip(tag.getResource(), colorMetric);
    link.getElement().setAttribute("title", tooltip);
    link.getElement().setAttribute("rel", tooltip);

    String sizeCss = Float.toString(maxSizePercent / 100f) + "em";
    link.setHeight(sizeCss);
    final Resource clickResource = tag.getResource();
    link.addClickHandler(new ClickHandler() {
      public void onClick(final ClickEvent event) {
        if (clickResource.getCopy() != null) {
          Window.Location.assign(Utils.getServerUrl() + "/plugins/resource/" + clickResource.getCopy() + "?page=" + GwtClouds.GWT_ID);
        } else {
          Utils.openResourcePopup(clickResource, colorMetric.getKey());
        }
      }
    });

    return link;
  }

  private String getTooltip(Resource resource, Metric colorMetric) {
    Measure sizeMeasure = getMeasure(resource, sizeMetric);
    String sizeMetricName = sizeMetric.getName();
    String sizeMetricValue = sizeMeasure.getFormattedValue();

    Measure colorMeasure = getMeasure(resource, colorMetric);
    String colorMetricName = colorMetric.getName();
    String colorMetricValue = colorMeasure.getFormattedValue();

    return resource.getName(true) + ", " + sizeMetricName + " : " + sizeMetricValue + ", " + colorMetricName + " : " + colorMetricValue;
  }
}
