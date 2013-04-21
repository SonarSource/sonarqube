/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.design.ui.libraries.client;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import org.sonar.gwt.Configuration;
import org.sonar.gwt.Links;
import org.sonar.wsclient.services.Resource;

public class Filters extends Grid {

  private static final String PARAM_TEST = "test";

  private KeywordFilter keywordFilter;
  private CheckBox testCheckbox;
  private Anchor expandCollapse;
  private boolean isExpanded;
  private Anchor usageLink;


  public Filters(Resource resource) {
    super(1, 5);

    setStyleName("libFilter");
    Dictionary l10n = Dictionary.getDictionary("l10n");

    keywordFilter = new KeywordFilter();
    setWidget(0, 0, new Label(l10n.get("libs.filter")));
    setWidget(0, 1, keywordFilter);

    testCheckbox = new CheckBox(l10n.get("libs.displayTests"));
    testCheckbox.getElement().setId("testCb");
    testCheckbox.setValue(Boolean.valueOf(Configuration.getRequestParameter(PARAM_TEST, "false")));
    setWidget(0, 2, testCheckbox);

    expandCollapse = new Anchor(l10n.get("libs.collapse"));
    isExpanded = true;
    setWidget(0, 3, expandCollapse);

    usageLink = new Anchor(l10n.get("libs.usageLink"), Links.baseUrl() + "/dependencies/index?search=" + resource.getKey());
    setWidget(0, 4, usageLink);
  }

  public KeywordFilter getKeywordFilter() {
    return keywordFilter;
  }

  public CheckBox getTestCheckbox() {
    return testCheckbox;
  }

  public boolean isTestDisplayed() {
    return testCheckbox.getValue();
  }

  public boolean isTestFiltered() {
    return !isTestDisplayed();
  }

  public boolean hasKeyword() {
    return getKeywordFilter().hasKeyword();
  }

  public Anchor getExpandCollapseLink() {
    return expandCollapse;
  }

  public boolean isExpanded() {
    return isExpanded;
  }

  public boolean isCollapsed() {
    return !isExpanded;
  }

  public Anchor getUsageLink() {
    return usageLink;
  }

  public void expand() {
    if (!isExpanded) {
      expandCollapse.setText(Dictionary.getDictionary("l10n").get("libs.collapse"));
      isExpanded = true;
    }
  }

  public void collapse() {
    if (isExpanded) {
      expandCollapse.setText(Dictionary.getDictionary("l10n").get("libs.expand"));
      isExpanded = false;
    }
  }

  public String toUrlParams() {
    return PARAM_TEST + '=' + testCheckbox.getValue() + '&' + KeywordFilter.PARAM_FILTER + '=' + getKeywordFilter();
  }
}
