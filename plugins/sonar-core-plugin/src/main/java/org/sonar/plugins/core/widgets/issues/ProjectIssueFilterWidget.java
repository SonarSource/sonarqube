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
package org.sonar.plugins.core.widgets.issues;

import org.sonar.api.web.*;
import org.sonar.plugins.core.widgets.CoreWidget;

import static org.sonar.api.web.WidgetScope.PROJECT;

@WidgetCategory({"Filters", "Issues"})
@WidgetScope(PROJECT)
@WidgetProperties({
  @WidgetProperty(key = ProjectIssueFilterWidget.FILTER_PROPERTY, type = WidgetPropertyType.ISSUE_FILTER, optional = false),
  @WidgetProperty(key = ProjectIssueFilterWidget.DISTRIBUTION_AXIS_PROPERTY, type = WidgetPropertyType.SINGLE_SELECT_LIST, defaultValue = "severities",
          options = {"severities", "resolutions", "statuses", "rules", "tags", "assignees", "reporters", "authors",
                  "languages", "actionPlans", "createdAt"}),
  @WidgetProperty(key = ProjectIssueFilterWidget.DISPLAY_FILTER_DESCRIPTION, type = WidgetPropertyType.BOOLEAN, defaultValue = "false")
})
public class ProjectIssueFilterWidget extends CoreWidget {

  public static final String FILTER_PROPERTY = "filter";
  public static final String DISTRIBUTION_AXIS_PROPERTY = "distributionAxis";
  public static final String DISPLAY_FILTER_DESCRIPTION = "displayFilterDescription";
  public static final String ID = "project_issue_filter";

  public ProjectIssueFilterWidget() {
    super(ID, "Project Issue Filter", "/org/sonar/plugins/core/widgets/issues/project_issue_filter.html.erb");
  }
}
