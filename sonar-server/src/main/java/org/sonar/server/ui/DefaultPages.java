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
package org.sonar.server.ui;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Resource;
import org.sonar.api.web.*;

/**
 * @since 2.7
 */
public final class DefaultPages {

  private static final View[] PAGES = {new SourceTab(), new CoverageTab(), new ViolationsTab()};

  public static View[] getPages() {
    return PAGES;
  }

  @ResourceScope(Resource.SCOPE_ENTITY)
  @NavigationSection(NavigationSection.RESOURCE_TAB)
  @DefaultTab
  @UserRole(UserRole.CODEVIEWER)
  private static final class SourceTab implements RubyRailsPage {
    public String getTemplate() {
      //not used, hardcoded in BrowseController
      return "browse/index";
    }

    public String getId() {
      return "source";
    }

    public String getTitle() {
      return "Source";
    }
  }


  @ResourceQualifier(Resource.QUALIFIER_CLASS)
  @NavigationSection(NavigationSection.RESOURCE_TAB)
  @DefaultTab(metrics = {CoreMetrics.COVERAGE_KEY, CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.UNCOVERED_LINES_KEY, CoreMetrics.LINE_COVERAGE_KEY, CoreMetrics.CONDITIONS_TO_COVER_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY, CoreMetrics.BRANCH_COVERAGE_KEY})
  @UserRole(UserRole.CODEVIEWER)
  private static final class CoverageTab implements RubyRailsPage {
    public String getTemplate() {
      //not used, hardcoded in BrowseController
      return "browse/index";
    }

    public String getId() {
      return "coverage";
    }

    public String getTitle() {
      return "Coverage";
    }
  }

  @NavigationSection(NavigationSection.RESOURCE_TAB)
  @DefaultTab(metrics = {CoreMetrics.VIOLATIONS_DENSITY_KEY, CoreMetrics.WEIGHTED_VIOLATIONS_KEY, CoreMetrics.VIOLATIONS_KEY, CoreMetrics.BLOCKER_VIOLATIONS_KEY, CoreMetrics.CRITICAL_VIOLATIONS_KEY, CoreMetrics.MAJOR_VIOLATIONS_KEY, CoreMetrics.MINOR_VIOLATIONS_KEY, CoreMetrics.INFO_VIOLATIONS_KEY})
  @ResourceQualifier({Resource.QUALIFIER_CLASS, Resource.QUALIFIER_FILE})
  @UserRole(UserRole.CODEVIEWER)
  private static final class ViolationsTab implements RubyRailsPage {
    public String getTemplate() {
      //not used, hardcoded in BrowseController
      return "browse/index";
    }

    public String getId() {
      return "violations";
    }

    public String getTitle() {
      return "Violations";
    }
  }
}
