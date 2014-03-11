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
package org.sonar.plugins.core.web;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.*;

@ResourceQualifier(Qualifiers.UNIT_TEST_FILE)
@NavigationSection(NavigationSection.RESOURCE_TAB)
@DefaultTab(metrics = {
    CoreMetrics.TESTS_KEY, CoreMetrics.TEST_EXECUTION_TIME_KEY, CoreMetrics.TEST_SUCCESS_DENSITY_KEY,
    CoreMetrics.TEST_FAILURES_KEY, CoreMetrics.TEST_ERRORS_KEY, CoreMetrics.SKIPPED_TESTS_KEY})
@UserRole(UserRole.USER)
public class TestsViewer extends AbstractRubyTemplate implements RubyRailsPage {

  public String getId() {
    return "tests_viewer";
  }

  public String getTitle() {
    return "Tests";
  }

  @Override
  protected String getTemplatePath() {
    return "/org/sonar/plugins/core/web/tests_viewer.html.erb";
  }
}
