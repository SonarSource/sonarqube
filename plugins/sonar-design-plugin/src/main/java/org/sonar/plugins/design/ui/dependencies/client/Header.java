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
package org.sonar.plugins.design.ui.dependencies.client;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.*;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

public class Header extends Composite {
  private FlowPanel header;

  public Header() {
    header = new FlowPanel();
    header.setStyleName("gwt-ViewerHeader");
    initWidget(header);
  }

  private void addMeasure(Panel panel, Resource resource, String key, String label) {
    Measure measure = resource.getMeasure(key);
    if (measure != null) {
      HTML html = new HTML(label + ": ");
      html.setStyleName("metric");
      panel.add(html);

      html = new HTML(measure.getFormattedValue("-"));
      html.setStyleName("value");
      panel.add(html);
    }
  }

  public void display(Data data) {
    header.clear();
    HorizontalPanel panel = new HorizontalPanel();
    header.add(panel);
    Dictionary l10n = Dictionary.getDictionary("l10n");
    addMeasure(panel, data.getResource(), "classes", l10n.get("depsTab.classes"));
    addMeasure(panel, data.getResource(), "dit", l10n.get("depsTab.dit"));
    addMeasure(panel, data.getResource(), "noc", l10n.get("depsTab.noc"));
    addMeasure(panel, data.getResource(), "rfc", l10n.get("depsTab.rfc"));
    addLcom4(data, panel);
  }

  private void addLcom4(Data data, HorizontalPanel panel) {
    Measure lcom4 = data.getResource().getMeasure("lcom4");
    if (lcom4 != null && lcom4.getIntValue()!=null) {
      HTML html = new HTML(Dictionary.getDictionary("l10n").get("depsTab.lcom4") + ": ");
      html.setStyleName("metric");
      panel.add(html);

      html = new HTML(lcom4.getIntValue() + "");
      html.setStyleName("value");
      if (lcom4.getIntValue()>1) {
        html.addStyleName("red bold");
      }

      panel.add(html);
    }
  }
}
