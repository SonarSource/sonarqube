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
package org.sonar.server.ui;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.DefaultTab;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.RequiredMeasures;
import org.sonar.api.web.ResourceQualifier;
import org.sonar.api.web.RubyRailsPage;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.View;

/**
 * @since 2.7
 */
public final class DefaultPages {

  private static final View[] PAGES = {new SourceTab(), new CoverageTab(), new ViolationsTab(), new DuplicationsTab()};

  private DefaultPages() {
  }

  public static View[] getPages() {
    return PAGES.clone();
  }

  // should be qualifier FILE only but waiting for java refactoring
  @NavigationSection(NavigationSection.RESOURCE_TAB)
  @DefaultTab
  @ResourceQualifier({Qualifiers.FILE, Qualifiers.CLASS, Qualifiers.UNIT_TEST_FILE})
  @UserRole(UserRole.CODEVIEWER)
  private static final class SourceTab implements RubyRailsPage {
    public String getTemplate() {
      // not used, hardcoded in BrowseController
      return "browse/index";
    }

    public String getId() {
      return "source";
    }

    public String getTitle() {
      return "Source";
    }
  }

  @NavigationSection(NavigationSection.RESOURCE_TAB)
  @ResourceQualifier({Qualifiers.FILE, Qualifiers.CLASS})
  @DefaultTab(
    metrics = {
      /* unit tests */
      CoreMetrics.COVERAGE_KEY, CoreMetrics.LINES_TO_COVER_KEY, CoreMetrics.UNCOVERED_LINES_KEY, CoreMetrics.LINE_COVERAGE_KEY,
      CoreMetrics.CONDITIONS_TO_COVER_KEY, CoreMetrics.UNCOVERED_CONDITIONS_KEY, CoreMetrics.BRANCH_COVERAGE_KEY,
      CoreMetrics.NEW_COVERAGE_KEY, CoreMetrics.NEW_UNCOVERED_LINES_KEY, CoreMetrics.NEW_LINE_COVERAGE_KEY,
      CoreMetrics.NEW_LINES_TO_COVER_KEY, CoreMetrics.NEW_BRANCH_COVERAGE_KEY, CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY,

      /* integration tests */
      CoreMetrics.IT_COVERAGE_KEY, CoreMetrics.IT_LINES_TO_COVER_KEY, CoreMetrics.IT_UNCOVERED_LINES_KEY, CoreMetrics.IT_LINE_COVERAGE_KEY,
      CoreMetrics.IT_CONDITIONS_TO_COVER_KEY, CoreMetrics.IT_UNCOVERED_CONDITIONS_KEY, CoreMetrics.IT_BRANCH_COVERAGE_KEY,
      CoreMetrics.NEW_IT_COVERAGE_KEY, CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY, CoreMetrics.NEW_IT_LINE_COVERAGE_KEY,
      CoreMetrics.NEW_IT_LINES_TO_COVER_KEY, CoreMetrics.NEW_IT_BRANCH_COVERAGE_KEY, CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY,

      /* system tests */
      CoreMetrics.SYSTEM_COVERAGE_KEY, CoreMetrics.SYSTEM_LINES_TO_COVER_KEY, CoreMetrics.SYSTEM_UNCOVERED_LINES_KEY, CoreMetrics.SYSTEM_LINE_COVERAGE_KEY,
      CoreMetrics.SYSTEM_CONDITIONS_TO_COVER_KEY, CoreMetrics.SYSTEM_UNCOVERED_CONDITIONS_KEY, CoreMetrics.SYSTEM_BRANCH_COVERAGE_KEY,
      CoreMetrics.NEW_SYSTEM_COVERAGE_KEY, CoreMetrics.NEW_SYSTEM_UNCOVERED_LINES_KEY, CoreMetrics.NEW_SYSTEM_LINE_COVERAGE_KEY,
      CoreMetrics.NEW_SYSTEM_LINES_TO_COVER_KEY, CoreMetrics.NEW_SYSTEM_BRANCH_COVERAGE_KEY, CoreMetrics.NEW_SYSTEM_CONDITIONS_TO_COVER_KEY, CoreMetrics.NEW_SYSTEM_UNCOVERED_CONDITIONS_KEY,
    
      /* overall tests */
      CoreMetrics.OVERALL_COVERAGE_KEY, CoreMetrics.OVERALL_LINES_TO_COVER_KEY, CoreMetrics.OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.OVERALL_LINE_COVERAGE_KEY,
      CoreMetrics.OVERALL_CONDITIONS_TO_COVER_KEY, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS_KEY, CoreMetrics.OVERALL_BRANCH_COVERAGE_KEY,
      CoreMetrics.NEW_OVERALL_COVERAGE_KEY, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY, CoreMetrics.NEW_OVERALL_LINE_COVERAGE_KEY,
      CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY, CoreMetrics.NEW_OVERALL_BRANCH_COVERAGE_KEY, CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY,
      CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY})
  @RequiredMeasures(anyOf = {CoreMetrics.COVERAGE_KEY, CoreMetrics.IT_COVERAGE_KEY, CoreMetrics.OVERALL_COVERAGE_KEY})
  @UserRole(UserRole.CODEVIEWER)
  private static final class CoverageTab implements RubyRailsPage {
    public String getTemplate() {
      // not used, hardcoded in BrowseController
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
  @DefaultTab(
    metrics = {CoreMetrics.VIOLATIONS_DENSITY_KEY, CoreMetrics.WEIGHTED_VIOLATIONS_KEY, CoreMetrics.VIOLATIONS_KEY, CoreMetrics.BLOCKER_VIOLATIONS_KEY,
      CoreMetrics.CRITICAL_VIOLATIONS_KEY, CoreMetrics.MAJOR_VIOLATIONS_KEY, CoreMetrics.MINOR_VIOLATIONS_KEY, CoreMetrics.INFO_VIOLATIONS_KEY,
      CoreMetrics.NEW_VIOLATIONS_KEY, CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY, CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY, CoreMetrics.NEW_MAJOR_VIOLATIONS_KEY,
      CoreMetrics.NEW_MINOR_VIOLATIONS_KEY, CoreMetrics.NEW_INFO_VIOLATIONS_KEY, CoreMetrics.ACTIVE_REVIEWS_KEY, CoreMetrics.UNASSIGNED_REVIEWS_KEY,
      CoreMetrics.UNPLANNED_REVIEWS_KEY, CoreMetrics.FALSE_POSITIVE_REVIEWS_KEY, CoreMetrics.UNREVIEWED_VIOLATIONS_KEY, CoreMetrics.NEW_UNREVIEWED_VIOLATIONS_KEY})
  @ResourceQualifier(
    value = {Qualifiers.VIEW, Qualifiers.SUBVIEW, Qualifiers.PROJECT, Qualifiers.MODULE, Qualifiers.PACKAGE, Qualifiers.DIRECTORY, Qualifiers.FILE, Qualifiers.CLASS,
      Qualifiers.UNIT_TEST_FILE})
  @UserRole(UserRole.CODEVIEWER)
  private static final class ViolationsTab implements RubyRailsPage {
    public String getTemplate() {
      // not used, hardcoded in BrowseController
      return "browse/index";
    }

    public String getId() {
      return "violations";
    }

    public String getTitle() {
      return "Violations";
    }
  }

  @NavigationSection(NavigationSection.RESOURCE_TAB)
  @DefaultTab(metrics = {CoreMetrics.DUPLICATED_LINES_KEY, CoreMetrics.DUPLICATED_BLOCKS_KEY, CoreMetrics.DUPLICATED_FILES_KEY, CoreMetrics.DUPLICATED_LINES_DENSITY_KEY})
  @ResourceQualifier({Qualifiers.FILE, Qualifiers.CLASS})
  @UserRole(UserRole.CODEVIEWER)
  private static final class DuplicationsTab implements RubyRailsPage {
    public String getTemplate() {
      // not used, hardcoded in BrowseController
      return "browse/index";
    }

    public String getId() {
      return "duplications";
    }

    public String getTitle() {
      return "Duplications";
    }
  }
}
