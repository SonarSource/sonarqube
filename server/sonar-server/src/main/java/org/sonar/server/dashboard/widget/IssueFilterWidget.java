/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.dashboard.widget;

import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;
import org.sonar.api.web.WidgetScope;

import static org.sonar.api.web.WidgetScope.GLOBAL;

@WidgetCategory({"Filters", "Issues"})
@WidgetScope(GLOBAL)
@WidgetProperties({
  @WidgetProperty(key = IssueFilterWidget.FILTER_PROPERTY, type = WidgetPropertyType.ISSUE_FILTER, optional = false),
  @WidgetProperty(key = IssueFilterWidget.DISTRIBUTION_AXIS_PROPERTY, type = WidgetPropertyType.SINGLE_SELECT_LIST, defaultValue = "severities",
    options = {"severities", "resolutions", "statuses", "rules", "tags", "projectUuids", "assignees", "reporters",
      "authors", "languages", "actionPlans", "createdAt"}),
  @WidgetProperty(key = IssueFilterWidget.DISPLAY_FILTER_DESCRIPTION, type = WidgetPropertyType.BOOLEAN, defaultValue = "false"),
  @WidgetProperty(key = IssueFilterWidget.DISPLAY_MODE, type = WidgetPropertyType.SINGLE_SELECT_LIST, defaultValue = "count", options = {"count", "debt"})
})
public class IssueFilterWidget extends CoreWidget {

  public static final String FILTER_PROPERTY = "filter";
  public static final String DISTRIBUTION_AXIS_PROPERTY = "distributionAxis";
  public static final String DISPLAY_FILTER_DESCRIPTION = "displayFilterDescription";
  public static final String DISPLAY_MODE = "displayMode";
  public static final String ID = "issue_filter";

  public IssueFilterWidget() {
    super(ID, "Issue Filter", "/org/sonar/server/dashboard/widget/issue_filter.html.erb");
  }
}
