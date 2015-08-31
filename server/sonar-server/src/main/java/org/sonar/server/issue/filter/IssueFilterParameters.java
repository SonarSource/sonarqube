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

package org.sonar.server.issue.filter;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.7
 */
public class IssueFilterParameters {

  public static final String ISSUES = "issues";
  public static final String SEVERITIES = "severities";
  public static final String STATUSES = "statuses";
  public static final String RESOLUTIONS = "resolutions";
  public static final String RESOLVED = "resolved";
  public static final String COMPONENTS = "components";
  public static final String COMPONENT_KEYS = "componentKeys";
  public static final String COMPONENT_UUIDS = "componentUuids";
  public static final String COMPONENT_ROOTS = "componentRoots";
  public static final String COMPONENT_ROOT_UUIDS = "componentRootUuids";
  public static final String MODULE_UUIDS = "moduleUuids";
  public static final String PROJECTS = "projects";
  public static final String PROJECT_KEYS = "projectKeys";
  public static final String PROJECT_UUIDS = "projectUuids";
  public static final String DIRECTORIES = "directories";
  public static final String FILE_UUIDS = "fileUuids";
  public static final String ON_COMPONENT_ONLY = "onComponentOnly";
  public static final String RULES = "rules";
  public static final String ACTION_PLANS = "actionPlans";
  public static final String REPORTERS = "reporters";
  public static final String ASSIGNEES = "assignees";
  public static final String AUTHORS = "authors";
  public static final String LANGUAGES = "languages";
  public static final String TAGS = "tags";
  public static final String ASSIGNED = "assigned";
  public static final String PLANNED = "planned";
  public static final String HIDE_RULES = "hideRules";
  public static final String HIDE_COMMENTS = "hideComments";
  public static final String CREATED_AFTER = "createdAfter";
  public static final String CREATED_AT = "createdAt";
  public static final String CREATED_BEFORE = "createdBefore";
  public static final String CREATED_IN_LAST = "createdInLast";
  public static final String PAGE_SIZE = "pageSize";
  public static final String PAGE_INDEX = "pageIndex";
  public static final String SORT = "sort";
  public static final String ASC = "asc";
  public static final String FACET_MODE = "facetMode";

  public static final String FACET_MODE_COUNT = "count";
  public static final String FACET_MODE_DEBT = "debt";

  public static final String FACET_ASSIGNED_TO_ME = "assigned_to_me";

  public static final List<String> ALL = ImmutableList.of(ISSUES, SEVERITIES, STATUSES, RESOLUTIONS, RESOLVED, COMPONENTS, COMPONENT_ROOTS, RULES, ACTION_PLANS, REPORTERS, TAGS,
    ASSIGNEES, LANGUAGES, ASSIGNED, PLANNED, HIDE_RULES, CREATED_AT, CREATED_AFTER, CREATED_BEFORE, CREATED_IN_LAST, COMPONENT_UUIDS, COMPONENT_ROOT_UUIDS, FACET_MODE,
    PROJECTS, PROJECT_UUIDS, PROJECT_KEYS, COMPONENT_KEYS, MODULE_UUIDS, DIRECTORIES, FILE_UUIDS, AUTHORS, HIDE_COMMENTS, PAGE_SIZE, PAGE_INDEX, SORT, ASC);

  static final List<String> ALL_WITHOUT_PAGINATION = newArrayList(Iterables.filter(ALL, new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return !PAGE_INDEX.equals(input) && !PAGE_SIZE.equals(input);
    }
  }));

  private IssueFilterParameters() {
    // Utility class
  }
}
