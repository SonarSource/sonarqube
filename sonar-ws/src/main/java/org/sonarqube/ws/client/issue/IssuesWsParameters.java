/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.ws.client.issue;

/**
 * @since 5.3
 */
public class IssuesWsParameters {

  public static final String CONTROLLER_ISSUES = "api/issues";

  public static final String ACTION_SEARCH = "search";
  public static final String ACTION_CHANGELOG = "changelog";
  public static final String ACTION_ADD_COMMENT = "add_comment";
  public static final String ACTION_EDIT_COMMENT = "edit_comment";
  public static final String ACTION_DELETE_COMMENT = "delete_comment";
  public static final String ACTION_ASSIGN = "assign";
  public static final String ACTION_DO_TRANSITION = "do_transition";
  public static final String ACTION_SET_SEVERITY = "set_severity";
  public static final String ACTION_COMPONENT_TAGS = "component_tags";
  public static final String ACTION_SET_TAGS = "set_tags";
  public static final String ACTION_SET_TYPE = "set_type";
  public static final String ACTION_BULK_CHANGE = "bulk_change";

  public static final String PARAM_ISSUE = "issue";
  public static final String PARAM_COMMENT = "comment";
  public static final String PARAM_TEXT = "text";
  public static final String PARAM_ASSIGNEE = "assignee";
  public static final String PARAM_TRANSITION = "transition";
  public static final String PARAM_SEVERITY = "severity";
  public static final String PARAM_COMPONENT_UUID = "componentUuid";
  public static final String PARAM_TYPE = "type";
  public static final String PARAM_ISSUES = "issues";
  public static final String PARAM_SEVERITIES = "severities";
  public static final String PARAM_STATUSES = "statuses";
  public static final String PARAM_RESOLUTIONS = "resolutions";
  public static final String PARAM_RESOLVED = "resolved";
  public static final String PARAM_COMPONENT_KEYS = "componentKeys";
  public static final String PARAM_COMPONENT_UUIDS = "componentUuids";
  public static final String PARAM_MODULE_UUIDS = "moduleUuids";
  public static final String PARAM_PROJECTS = "projects";
  public static final String PARAM_PROJECT_KEYS = "projectKeys";
  public static final String PARAM_DIRECTORIES = "directories";
  public static final String PARAM_FILE_UUIDS = "fileUuids";
  public static final String PARAM_ON_COMPONENT_ONLY = "onComponentOnly";
  public static final String PARAM_BRANCH = "branch";
  public static final String PARAM_PULL_REQUEST = "pullRequest";
  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_RULES = "rules";
  public static final String PARAM_ASSIGN = "assign";
  public static final String PARAM_SET_SEVERITY = "set_severity";
  public static final String PARAM_SET_TYPE = "set_type";
  public static final String PARAM_DO_TRANSITION = "do_transition";
  public static final String PARAM_ADD_TAGS = "add_tags";
  public static final String PARAM_REMOVE_TAGS = "remove_tags";
  public static final String PARAM_SEND_NOTIFICATIONS = "sendNotifications";
  public static final String PARAM_ASSIGNEES = "assignees";

  /**
   * @deprecated since 7.7, please use 'author' instead
   */
  @Deprecated
  public static final String DEPRECATED_PARAM_AUTHORS = "authors";

  public static final String PARAM_AUTHOR = "author";
  public static final String PARAM_LANGUAGES = "languages";
  public static final String PARAM_TAGS = "tags";
  public static final String PARAM_TYPES = "types";
  public static final String PARAM_OWASP_TOP_10 = "owaspTop10";
  public static final String PARAM_SANS_TOP_25 = "sansTop25";
  public static final String PARAM_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  public static final String PARAM_CWE = "cwe";
  public static final String PARAM_ASSIGNED = "assigned";
  public static final String PARAM_HIDE_COMMENTS = "hideComments";
  public static final String PARAM_CREATED_AFTER = "createdAfter";
  public static final String PARAM_CREATED_AT = "createdAt";
  public static final String PARAM_CREATED_BEFORE = "createdBefore";
  public static final String PARAM_CREATED_IN_LAST = "createdInLast";
  public static final String PARAM_SINCE_LEAK_PERIOD = "sinceLeakPeriod";
  public static final String PARAM_PAGE_SIZE = "pageSize";
  public static final String PARAM_PAGE_INDEX = "pageIndex";
  public static final String PARAM_ASC = "asc";
  public static final String PARAM_ADDITIONAL_FIELDS = "additionalFields";

  /**
   * @deprecated since 7.9
   */
  @Deprecated
  public static final String FACET_MODE = "facetMode";
  public static final String FACET_MODE_COUNT = "count";
  public static final String FACET_MODE_EFFORT = "effort";

  private IssuesWsParameters() {
    // Utility class
  }
}
