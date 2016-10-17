/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

/**
 * Definition of ES index "issues", including settings and fields.
 */
public class IssueIndexDefinition implements IndexDefinition {

  public static final String INDEX = "issues";

  public static final String TYPE_AUTHORIZATION = "authorization";
  public static final String TYPE_ISSUE = "issue";

  public static final String FIELD_AUTHORIZATION_PROJECT_UUID = "project";
  public static final String FIELD_AUTHORIZATION_GROUPS = "groups";
  public static final String FIELD_AUTHORIZATION_USERS = "users";
  public static final String FIELD_AUTHORIZATION_UPDATED_AT = "updatedAt";

  public static final String FIELD_ISSUE_ASSIGNEE = "assignee";
  public static final String FIELD_ISSUE_ATTRIBUTES = "attributes";
  public static final String FIELD_ISSUE_AUTHOR_LOGIN = "authorLogin";
  public static final String FIELD_ISSUE_COMPONENT_UUID = "component";
  public static final String FIELD_ISSUE_EFFORT = "effort";
  public static final String FIELD_ISSUE_GAP = "gap";
  public static final String FIELD_ISSUE_FILE_PATH = "filePath";
  /**
   * Functional date
   */
  public static final String FIELD_ISSUE_FUNC_CREATED_AT = "issueCreatedAt";
  /**
   * Functional date
   */
  public static final String FIELD_ISSUE_FUNC_UPDATED_AT = "issueUpdatedAt";
  /**
   * Functional date
   */
  public static final String FIELD_ISSUE_FUNC_CLOSED_AT = "issueClosedAt";
  public static final String FIELD_ISSUE_KEY = "key";
  public static final String FIELD_ISSUE_LANGUAGE = "language";
  public static final String FIELD_ISSUE_LINE = "line";
  public static final String FIELD_ISSUE_MESSAGE = "message";
  public static final String FIELD_ISSUE_MODULE_UUID = "module";
  public static final String FIELD_ISSUE_MODULE_PATH = "modulePath";
  public static final String FIELD_ISSUE_PROJECT_UUID = "project";
  public static final String FIELD_ISSUE_DIRECTORY_PATH = "dirPath";
  public static final String FIELD_ISSUE_RESOLUTION = "resolution";
  public static final String FIELD_ISSUE_RULE_KEY = "ruleKey";
  public static final String FIELD_ISSUE_SEVERITY = "severity";
  public static final String FIELD_ISSUE_SEVERITY_VALUE = "severityValue";
  public static final String FIELD_ISSUE_MANUAL_SEVERITY = "manualSeverity";
  public static final String FIELD_ISSUE_STATUS = "status";
  public static final String FIELD_ISSUE_CHECKSUM = "checksum";
  public static final String FIELD_ISSUE_TAGS = "tags";
  public static final String FIELD_ISSUE_TYPE = "type";
  /**
   * Technical date
   */
  public static final String FIELD_ISSUE_TECHNICAL_UPDATED_AT = "updatedAt";

  private final Settings settings;

  public IssueIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);

    index.refreshHandledByIndexer();
    index.configureShards(settings, 5);

    // type "issue"
    NewIndex.NewIndexType issueMapping = index.createType(TYPE_ISSUE);
    issueMapping.setAttribute("_parent", ImmutableMap.of("type", TYPE_AUTHORIZATION));
    issueMapping.setAttribute("_routing", ImmutableMap.of("required", "true"));
    issueMapping.stringFieldBuilder(FIELD_ISSUE_ASSIGNEE).disableNorms().enableSorting().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_ATTRIBUTES).disableNorms().disableSearch().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_AUTHOR_LOGIN).disableNorms().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_COMPONENT_UUID).disableNorms().build();
    issueMapping.createLongField(FIELD_ISSUE_EFFORT);
    issueMapping.createDoubleField(FIELD_ISSUE_GAP);
    issueMapping.stringFieldBuilder(FIELD_ISSUE_FILE_PATH).disableNorms().enableSorting().build();
    issueMapping.createDateTimeField(FIELD_ISSUE_FUNC_CREATED_AT);
    issueMapping.createDateTimeField(FIELD_ISSUE_FUNC_UPDATED_AT);
    issueMapping.createDateTimeField(FIELD_ISSUE_FUNC_CLOSED_AT);
    issueMapping.stringFieldBuilder(FIELD_ISSUE_KEY).disableNorms().enableSorting().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_LANGUAGE).disableNorms().build();
    issueMapping.createIntegerField(FIELD_ISSUE_LINE);
    issueMapping.stringFieldBuilder(FIELD_ISSUE_MESSAGE).disableNorms().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_MODULE_UUID).disableNorms().build();
    issueMapping.createUuidPathField(FIELD_ISSUE_MODULE_PATH);
    issueMapping.stringFieldBuilder(FIELD_ISSUE_PROJECT_UUID).disableNorms().enableSorting().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_DIRECTORY_PATH).disableNorms().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_RESOLUTION).disableNorms().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_RULE_KEY).disableNorms().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_SEVERITY).disableNorms().build();
    issueMapping.createByteField(FIELD_ISSUE_SEVERITY_VALUE);
    issueMapping.stringFieldBuilder(FIELD_ISSUE_STATUS).disableNorms().enableSorting().build();
    issueMapping.stringFieldBuilder(FIELD_ISSUE_TAGS).disableNorms().build();
    issueMapping.createDateTimeField(FIELD_ISSUE_TECHNICAL_UPDATED_AT);
    issueMapping.stringFieldBuilder(FIELD_ISSUE_TYPE).disableNorms().build();

    // type "authorization"
    NewIndex.NewIndexType authorizationMapping = index.createType(TYPE_AUTHORIZATION);
    authorizationMapping.setAttribute("_routing", ImmutableMap.of("required", "true"));
    authorizationMapping.createDateTimeField(FIELD_AUTHORIZATION_UPDATED_AT);
    authorizationMapping.stringFieldBuilder(FIELD_AUTHORIZATION_PROJECT_UUID).disableNorms().build();
    authorizationMapping.stringFieldBuilder(FIELD_AUTHORIZATION_GROUPS).disableNorms().build();
    authorizationMapping.stringFieldBuilder(FIELD_AUTHORIZATION_USERS).disableNorms().build();
    // do not store document but only indexation of information
    authorizationMapping.setEnableSource(false);
  }
}
