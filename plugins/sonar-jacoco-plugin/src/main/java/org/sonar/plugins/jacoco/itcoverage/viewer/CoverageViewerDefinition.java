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
package org.sonar.plugins.jacoco.itcoverage.viewer;

import org.sonar.api.resources.Resource;
import org.sonar.api.web.*;
import org.sonar.plugins.jacoco.itcoverage.JaCoCoItMetrics;

@ResourceQualifier(Resource.QUALIFIER_CLASS)
@NavigationSection(NavigationSection.RESOURCE_TAB)
@DefaultTab(metrics = { JaCoCoItMetrics.IT_COVERAGE_KEY, JaCoCoItMetrics.IT_LINES_TO_COVER_KEY, JaCoCoItMetrics.IT_UNCOVERED_LINES_KEY, JaCoCoItMetrics.IT_LINE_COVERAGE_KEY, JaCoCoItMetrics.IT_CONDITIONS_TO_COVER_KEY, JaCoCoItMetrics.IT_UNCOVERED_CONDITIONS_KEY, JaCoCoItMetrics.IT_BRANCH_COVERAGE_KEY })
@UserRole(UserRole.CODEVIEWER)
public class CoverageViewerDefinition extends GwtPage {

  public String getTitle() {
    return "IT Coverage";
  }

  public String getGwtId() {
    return "org.sonar.plugins.jacoco.itcoverage.viewer.CoverageViewer";
  }
}
