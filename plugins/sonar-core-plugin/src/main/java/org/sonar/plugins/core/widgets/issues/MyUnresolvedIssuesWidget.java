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
package org.sonar.plugins.core.widgets.issues;

import org.sonar.api.web.*;
import org.sonar.plugins.core.widgets.CoreWidget;

import static org.sonar.api.web.WidgetScope.GLOBAL;

@WidgetCategory({"Issues"})
@WidgetScope(GLOBAL)
@WidgetProperties({
  @WidgetProperty(key = "numberOfLines", type = WidgetPropertyType.INTEGER, defaultValue = "5")
})
public class MyUnresolvedIssuesWidget extends CoreWidget {
  public MyUnresolvedIssuesWidget() {
    super("my_reviews", "My unresolved issues", "/org/sonar/plugins/core/widgets/issues/my_unresolved_issues.html.erb");
  }
}
