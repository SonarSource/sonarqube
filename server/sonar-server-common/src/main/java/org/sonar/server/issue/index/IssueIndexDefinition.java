/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import javax.inject.Inject;
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
  public static final String FIELD_ISSUE_SCOPE = "scope";
  public static final String FIELD_ISSUE_LANGUAGE = "language";
  public static final String FIELD_ISSUE_LINE = "line";
  public static final String FIELD_ISSUE_ORGANIZATION_UUID = "organization";

  /**
   * The (real) project, equivalent of projects.uuid, so
   * it's never empty.
   * This field maps the parent association with issues/authorization.
   */
  public static final String FIELD_ISSUE_PROJECT_UUID = "project";

  /**
   * The branch. It's never empty. It maps the DB column components.branch_uuid.
   * It's the UUID of the branch and it's different than {@link #FIELD_ISSUE_PROJECT_UUID}.
   */
  public static final String FIELD_ISSUE_BRANCH_UUID = "branch";

  /**
   * Whether component is in a main branch or not.
   */
  public static final String FIELD_ISSUE_IS_MAIN_BRANCH = "isMainBranch";

  public static final String FIELD_ISSUE_DIRECTORY_PATH = "dirPath";
  public static final String FIELD_ISSUE_RESOLUTION = "resolution";
  public static final String FIELD_ISSUE_RULE_UUID = "ruleUuid";
  public static final String FIELD_ISSUE_SEVERITY = "severity";
  public static final String FIELD_ISSUE_SEVERITY_VALUE = "severityValue";
  public static final String FIELD_ISSUE_STATUS = "status";
  public static final String FIELD_ISSUE_NEW_STATUS = "issueStatus";
  public static final String FIELD_ISSUE_TAGS = "tags";
  public static final String FIELD_ISSUE_TYPE = "type";
  public static final String FIELD_ISSUE_PCI_DSS_32 = "pciDss-3.2";
  public static final String FIELD_ISSUE_PCI_DSS_40 = "pciDss-4.0";
  public static final String FIELD_ISSUE_OWASP_ASVS_40 = "owaspAsvs-4.0";
  public static final String FIELD_ISSUE_OWASP_ASVS_40_LEVEL = "owaspAsvs-4.0-level";
  public static final String FIELD_ISSUE_OWASP_TOP_10 = "owaspTop10";
  public static final String FIELD_ISSUE_OWASP_TOP_10_2021 = "owaspTop10-2021";
  public static final String FIELD_ISSUE_SANS_TOP_25 = "sansTop25";
  public static final String FIELD_ISSUE_CWE = "cwe";
  public static final String FIELD_ISSUE_STIG_ASD_V5R3 = "stig-ASD_V5R3";
  public static final String FIELD_ISSUE_CASA = "casa";
  public static final String FIELD_ISSUE_SQ_SECURITY_CATEGORY = "sonarsourceSecurity";
  public static final String FIELD_ISSUE_VULNERABILITY_PROBABILITY = "vulnerabilityProbability";
  public static final String FIELD_ISSUE_CODE_VARIANTS = "codeVariants";

  /**
   * Whether issue is new code for a branch using the reference branch new code definition.
   */
  public static final String FIELD_ISSUE_NEW_CODE_REFERENCE = "isNewCodeReference";
  public static final String FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY = "cleanCodeAttributeCategory";
  public static final String FIELD_ISSUE_IMPACTS = "impacts";
  public static final String SUB_FIELD_SOFTWARE_QUALITY = "softwareQuality";
  public static final String SUB_FIELD_SEVERITY = "severity";
  public static final String FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY = FIELD_ISSUE_IMPACTS + "." + SUB_FIELD_SOFTWARE_QUALITY;
  public static final String FIELD_ISSUE_IMPACT_SEVERITY = FIELD_ISSUE_IMPACTS + "." + SUB_FIELD_SEVERITY;
  public static final String FIELD_PRIORITIZED_RULE = "prioritizedRule";

  private final Configuration config;
  private final boolean enableSource;

  @Inject
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
    mapping.keywordFieldBuilder(FIELD_ISSUE_SCOPE).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_LANGUAGE).disableNorms().build();
    mapping.createIntegerField(FIELD_ISSUE_LINE);
    mapping.keywordFieldBuilder(FIELD_ISSUE_ORGANIZATION_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_PROJECT_UUID).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_BRANCH_UUID).disableNorms().build();
    mapping.createBooleanField(FIELD_ISSUE_IS_MAIN_BRANCH);
    mapping.keywordFieldBuilder(FIELD_ISSUE_DIRECTORY_PATH).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_RESOLUTION).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_NEW_STATUS).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_RULE_UUID).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_SEVERITY).disableNorms().build();
    mapping.createByteField(FIELD_ISSUE_SEVERITY_VALUE);
    mapping.keywordFieldBuilder(FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY).disableNorms().build();
    mapping.nestedFieldBuilder(FIELD_ISSUE_IMPACTS)
      .addKeywordField(SUB_FIELD_SOFTWARE_QUALITY)
      .addKeywordField(SUB_FIELD_SEVERITY)
      .build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_STATUS).disableNorms().addSubFields(SORTABLE_ANALYZER).build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_TAGS).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_TYPE).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_PCI_DSS_32).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_PCI_DSS_40).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_OWASP_ASVS_40).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_OWASP_ASVS_40_LEVEL).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_OWASP_TOP_10).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_OWASP_TOP_10_2021).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_SANS_TOP_25).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_CWE).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_SQ_SECURITY_CATEGORY).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_VULNERABILITY_PROBABILITY).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_STIG_ASD_V5R3).disableNorms().build();
    mapping.keywordFieldBuilder(FIELD_ISSUE_CASA).disableNorms().build();
    mapping.createBooleanField(FIELD_ISSUE_NEW_CODE_REFERENCE);
    mapping.keywordFieldBuilder(FIELD_ISSUE_CODE_VARIANTS).disableNorms().build();
    mapping.createBooleanField(FIELD_PRIORITIZED_RULE);
  }
}
