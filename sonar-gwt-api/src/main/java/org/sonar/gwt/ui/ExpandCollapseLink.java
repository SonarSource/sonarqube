/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

public class ExpandCollapseLink extends Hyperlink {

  private Widget expandOrCollapse;

  public ExpandCollapseLink(Widget expandOrCollapse) {
    super();
    this.expandOrCollapse = expandOrCollapse;
    setText(getLinkLabel(!expandOrCollapse.isVisible()));
    getElement().setId("expand-" + expandOrCollapse.getElement().getId());
    setStyleName("expandCollapseLink");
    final ExpandCollapseLink link = this;
    this.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        link.toggle();
      }
    });
    getElement().getFirstChildElement().setAttribute("href", "#");
  }

  public void toggle() {
    boolean visible = expandOrCollapse.isVisible();
    setText(getLinkLabel(visible));
    expandOrCollapse.setVisible(!visible);
  }

  protected String getLinkLabel(boolean show) {
    return (show ? "expand" : "collapse");
  }

}