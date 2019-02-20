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
package org.sonar.server.issue.index;

import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.newindex.NewAuthorizedIndex;
import org.sonar.server.es.newindex.TypeMapping;

import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.newindex.SettingsConfiguration.MANUAL_REFRESH_INTERVAL;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;
import static org.sonar.server.permission.index.IndexAuthorizationConstants.TYPE_AUTHORIZATION;

/**
 * Definition of ES index "issues", including settings and fields.
 */
public class IssueIndexDefinition implements IndexDefinition {

  public static final Index DESCRIPTOR = Index.withRelations("issues");
  public static final IndexType.IndexRelationType TYPE_ISSUE = IndexType.relation(IndexType.main(DESCRIPTOR, TYPE_AUTHORIZATION), "issue");
  public static final String FIELD_ISSUE_ASSIGNEE_UUID = "assignee";
  public static final String FIELD_ISSUE_AUTHOR_LOGIN = "authorLogin";
  public static final String FIELD_ISSUE_COMPONENT_UUID = "component";
  public static final String FIELD_ISSUE_EFFORT = "effort";
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
  public static final String FIELD_ISSUE_MODULE_UUID = "module";
  public static final String FIELD_ISSUE_MODULE_PATH = "modulePath";
  public static final String FIELD_ISSUE_ORGANIZATION_UUID = "organization";

  /**
   * The (real) project, equivalent of projects.main_branch_project_uuid | projects.project_uuid, so
   * it's never empty.
   * On main branches, it is the UUID of the project.
   * On non-main branches, it is the UUID of the main branch (which represents the project).
   * This field maps the parent association with issues/authorization.
   */
  public static final String FIELD_ISSUE_PROJECT_UUID = "project";

  /**
   * The branch, as represented by the component with TRK qualifier. It's never
   * empty. It maps the DB column projects.project_uuid:
   * - on main branches, it is the UUID of the project. It equals {@link #FIELD_ISSUE_PROJECT_UUID}.
   * - on non-main branches, it is the UUID of the project representing the branch and it
   * is different than {@link #FIELD_ISSUE_PROJECT_UUID}.
   */
  public static final String FIELD_ISSUE_BRANCH_UUID = "branch";

  /**
   * Whether component is in a main branch or not.
   * If true, then {@link #FIELD_ISSUE_BRANCH_UUID} equals {@link #FIELD_ISSUE_PROJECT_UUID}.
   * If false, then {@link #FIELD_ISSUE_BRANCH_UUID} is different than {@link #FIELD_ISSUE_PROJECT_UUID}.
   */
  public static final String FIELD_ISSUE_IS_MAIN_BRANCH = "isMainBranch";

  public static final String FIELD_ISSUE_DIRECTORY_PATH = "dirPath";
  public static final String FIELD_ISSUE_RESOLUTION = "resolution";
  public static final String FIELD_ISSUE_RULE_ID = "ruleId";
  public static final String FIELD_ISSUE_SEVERITY = "severity";
  public static final String FIELD_ISSUE_SEVERITY_VALUE = "severityValue";
  public static final String FIELD_ISSUE_STATUS = "status";
  public static final String FIELD_ISSUE_TAGS = "tags";
  public static final String FIELD_ISSUE_TYPE = "type";
  public static final String FIELD_ISSUE_OWASP_TOP_10 = "owaspTop10";
  public static final String FIELD_ISSUE_SANS_TOP_25 = "sansTop25";
  public static final String FIELD_ISSUE_CWE = "cwe";

  private final Configuration config;
  private final boolean enableSource;

  public IssueIndexDefinition(Configuration config) {
    this(config, false);
  }

  private IssueIndexDefinition(Configuration config, boolean enableSource) {
    this.config = config;
    this.enableSource = enableSource;
  }

  /**
   * Keep the document sources in index so that indexer tests can verify content
   * of indexed documents.
   */
  public static IssueIndexDefinition createForTest() {
    return new IssueIndexDefinition(new MapSettings().asConfig(), true);
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewAuthorizedIndex index = context.createWithAuthorization(
      DESCRIPTOR,
      newBuilder(config)
        .setRefreshInterval(MANUAL_REFRESH_INTERVAL)
        .setDefaultNbOfShards(5)
        .build())
      .setEnableSource(enableSource);

    TypeMapping mapping = index.createTypeMapping(TYPE_ISSUE);
    mapping.keywordFieldBuilder(FIELD_ISSUE_ASSIGNEE_UUID).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_AUTHOR_LOGIN).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_COMPONENT_UUID).disableNorms().build();
    mapping.createLongField(FIELD_ISSUE_EFFORT);
    mapping.keywordFieldBuilder(FIELD_ISSUE_FILE_PATH).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.createDateTimeField(FIELD_ISSUE_FUNC_CREATED_AT);
    mapping.createDateTimeField(FIELD_ISSUE_FUNC_UPDATED_AT);
    mapping.createDateTimeField(FIELD_ISSUE_FUNC_CLOSED_AT);
    mapping.keywordFieldBuilder(FIELD_ISSUE_KEY).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_LANGUAGE).disableNorms().build();
    mapping.createIntegerField(FIELD_ISSUE_LINE);
    mapping.keywordFieldBuilder(FIELD_ISSUE_MODULE_UUID).disableNorms().build();
    mapping.createUuidPathField(FIELD_ISSUE_MODULE_PATH);
    mapping.keywordFieldBuilder(FIELD_ISSUE_ORGANIZATION_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_PROJECT_UUID).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_BRANCH_UUID).disableNorms().build();
    mapping.createBooleanField(FIELD_ISSUE_IS_MAIN_BRANCH);
    mapping.keywordFieldBuilder(FIELD_ISSUE_DIRECTORY_PATH).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_RESOLUTION).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_RULE_ID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_SEVERITY).disableNorms().build();
    mapping.createByteField(FIELD_ISSUE_SEVERITY_VALUE);
    mapping.keywordFieldBuilder(FIELD_ISSUE_STATUS).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_TAGS).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_TYPE).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_OWASP_TOP_10).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_SANS_TOP_25).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_CWE).disableNorms().build();
  }
}
