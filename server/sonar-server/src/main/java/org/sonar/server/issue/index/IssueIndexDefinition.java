/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue.index;

import org.sonar.api.config.Configuration;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;

import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

/**
 * Definition of ES index "issues", including settings and fields.
 */
public class IssueIndexDefinition implements IndexDefinition {

  public static final IndexType INDEX_TYPE_ISSUE = new IndexType("issues", "issue");
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
  public static final String FIELD_ISSUE_ORGANIZATION_UUID = "organization";
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

  private final Configuration config;

  public IssueIndexDefinition(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_TYPE_ISSUE.getIndex());

    index.refreshHandledByIndexer();
    index.configureShards(config, 5);

    NewIndex.NewIndexType type = index.createType(INDEX_TYPE_ISSUE.getType());
    type.requireProjectAuthorization();

    type.stringFieldBuilder(FIELD_ISSUE_ASSIGNEE).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    type.stringFieldBuilder(FIELD_ISSUE_ATTRIBUTES).disableNorms().disableSearch().build();
    type.stringFieldBuilder(FIELD_ISSUE_AUTHOR_LOGIN).disableNorms().build();
    type.stringFieldBuilder(FIELD_ISSUE_COMPONENT_UUID).disableNorms().build();
    type.createLongField(FIELD_ISSUE_EFFORT);
    type.createDoubleField(FIELD_ISSUE_GAP);
    type.stringFieldBuilder(FIELD_ISSUE_FILE_PATH).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    type.createDateTimeField(FIELD_ISSUE_FUNC_CREATED_AT);
    type.createDateTimeField(FIELD_ISSUE_FUNC_UPDATED_AT);
    type.createDateTimeField(FIELD_ISSUE_FUNC_CLOSED_AT);
    type.stringFieldBuilder(FIELD_ISSUE_KEY).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    type.stringFieldBuilder(FIELD_ISSUE_LANGUAGE).disableNorms().build();
    type.createIntegerField(FIELD_ISSUE_LINE);
    type.stringFieldBuilder(FIELD_ISSUE_MESSAGE).disableNorms().build();
    type.stringFieldBuilder(FIELD_ISSUE_MODULE_UUID).disableNorms().build();
    type.createUuidPathField(FIELD_ISSUE_MODULE_PATH);
    type.stringFieldBuilder(FIELD_ISSUE_ORGANIZATION_UUID).disableNorms().build();
    type.stringFieldBuilder(FIELD_ISSUE_PROJECT_UUID).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    type.stringFieldBuilder(FIELD_ISSUE_DIRECTORY_PATH).disableNorms().build();
    type.stringFieldBuilder(FIELD_ISSUE_RESOLUTION).disableNorms().build();
    type.stringFieldBuilder(FIELD_ISSUE_RULE_KEY).disableNorms().build();
    type.stringFieldBuilder(FIELD_ISSUE_SEVERITY).disableNorms().build();
    type.createByteField(FIELD_ISSUE_SEVERITY_VALUE);
    type.stringFieldBuilder(FIELD_ISSUE_STATUS).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    type.stringFieldBuilder(FIELD_ISSUE_TAGS).disableNorms().build();
    type.createDateTimeField(FIELD_ISSUE_TECHNICAL_UPDATED_AT);
    type.stringFieldBuilder(FIELD_ISSUE_TYPE).disableNorms().build();
  }
}
