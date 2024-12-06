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
package org.sonar.server.rule.index;

import java.util.Set;
import javax.inject.Inject;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.IndexType.IndexRelationType;
import org.sonar.server.es.newindex.NewRegularIndex;
import org.sonar.server.es.newindex.TypeMapping;

import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.ENGLISH_HTML_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.newindex.SettingsConfiguration.MANUAL_REFRESH_INTERVAL;
import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

/**
 * Definition of ES index "rules", including settings and fields.
 */
public class RuleIndexDefinition implements IndexDefinition {

  public static final Index DESCRIPTOR = Index.withRelations("rules");
  public static final IndexMainType TYPE_RULE = IndexType.main(DESCRIPTOR, "rule");
  public static final String FIELD_RULE_UUID = "ruleUuid";
  public static final String FIELD_RULE_KEY = "key";
  public static final String FIELD_RULE_REPOSITORY = "repo";
  public static final String FIELD_RULE_RULE_KEY = "ruleKey";
  public static final String FIELD_RULE_INTERNAL_KEY = "internalKey";
  public static final String FIELD_RULE_NAME = "name";
  public static final String FIELD_RULE_HTML_DESCRIPTION = "htmlDesc";
  public static final String FIELD_RULE_SEVERITY = "severity";
  public static final String FIELD_RULE_STATUS = "status";
  public static final String FIELD_RULE_LANGUAGE = "lang";
  public static final String FIELD_RULE_IS_TEMPLATE = "isTemplate";
  public static final String FIELD_RULE_IS_EXTERNAL = "isExternal";
  public static final String FIELD_RULE_TEMPLATE_KEY = "templateKey";
  public static final String FIELD_RULE_TYPE = "type";
  public static final String FIELD_RULE_CREATED_AT = "createdAt";
  public static final String FIELD_RULE_UPDATED_AT = "updatedAt";
  public static final String FIELD_RULE_CWE = "cwe";
  public static final String FIELD_RULE_OWASP_TOP_10 = "owaspTop10";
  public static final String FIELD_RULE_OWASP_TOP_10_2021 = "owaspTop10-2021";
  public static final String FIELD_RULE_SANS_TOP_25 = "sansTop25";
  public static final String FIELD_RULE_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  public static final String FIELD_RULE_TAGS = "tags";
  public static final String FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY = "cleanCodeAttributeCategory";
  public static final String FIELD_RULE_IMPACTS = "impacts";
  public static final String SUB_FIELD_SOFTWARE_QUALITY = "softwareQuality";
  public static final String SUB_FIELD_SEVERITY = "severity";
  public static final String FIELD_RULE_IMPACT_SOFTWARE_QUALITY = FIELD_RULE_IMPACTS + "." + SUB_FIELD_SOFTWARE_QUALITY;
  public static final String FIELD_RULE_IMPACT_SEVERITY = FIELD_RULE_IMPACTS + "." + SUB_FIELD_SEVERITY;

  // Active rule fields
  public static final IndexRelationType TYPE_ACTIVE_RULE = IndexType.relation(TYPE_RULE, "activeRule");
  public static final String FIELD_ACTIVE_RULE_UUID = "activeRule_uuid";
  public static final String FIELD_ACTIVE_RULE_INHERITANCE = "activeRule_inheritance";
  public static final String FIELD_ACTIVE_RULE_PROFILE_UUID = "activeRule_ruleProfile";
  public static final String FIELD_ACTIVE_RULE_SEVERITY = "activeRule_severity";
  public static final String FIELD_PRIORITIZED_RULE = "activeRule_prioritizedRule";
  public static final String FIELD_ACTIVE_RULE_IMPACTS = "activeRule_impacts";
  public static final String FIELD_ACTIVE_RULE_IMPACT_SOFTWARE_QUALITY = FIELD_ACTIVE_RULE_IMPACTS + "." + SUB_FIELD_SOFTWARE_QUALITY;
  public static final String FIELD_ACTIVE_RULE_IMPACT_SEVERITY = FIELD_ACTIVE_RULE_IMPACTS + "." + SUB_FIELD_SEVERITY;

  public static final Set<String> SORT_FIELDS = Set.of(
    FIELD_RULE_NAME,
    FIELD_RULE_UPDATED_AT,
    FIELD_RULE_CREATED_AT,
    FIELD_RULE_KEY);

  private final Configuration config;
  private final boolean enableSource;

  @Inject
  public RuleIndexDefinition(Configuration config) {
    this(config, false);
  }

  private RuleIndexDefinition(Configuration config, boolean enableSource) {
    this.config = config;
    this.enableSource = enableSource;
  }

  /**
   * Keep the document sources in index so that indexer tests can verify content
   * of indexed documents.
   */
  public static RuleIndexDefinition createForTest() {
    return new RuleIndexDefinition(new MapSettings().asConfig(), true);
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewRegularIndex index = context.create(
      DESCRIPTOR,
      newBuilder(config)
        .setRefreshInterval(MANUAL_REFRESH_INTERVAL)
        // Default nb of shards should be greater than 1 in order to
        // easily detect routing misconfiguration.
        // See https://jira.sonarsource.com/browse/SONAR-9489
        .setDefaultNbOfShards(2)
        .build())
      .setEnableSource(enableSource);

    // Rule type
    TypeMapping ruleMapping = index.createTypeMapping(TYPE_RULE);
    ruleMapping.keywordFieldBuilder(FIELD_RULE_UUID).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_KEY).addSubFields(SORTABLE_ANALYZER).build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_RULE_KEY).addSubFields(SORTABLE_ANALYZER).build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_REPOSITORY).build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_INTERNAL_KEY).disableNorms().disableSearch().build();

    ruleMapping.keywordFieldBuilder(FIELD_RULE_NAME).addSubFields(SORTABLE_ANALYZER, SEARCH_GRAMS_ANALYZER).build();
    ruleMapping.textFieldBuilder(FIELD_RULE_HTML_DESCRIPTION)
      .disableSearch()
      .disableNorms()
      .addSubFields(ENGLISH_HTML_ANALYZER)
      .build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_SEVERITY).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_STATUS).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_LANGUAGE).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_TAGS).build();

    ruleMapping.createBooleanField(FIELD_RULE_IS_TEMPLATE);
    ruleMapping.createBooleanField(FIELD_RULE_IS_EXTERNAL);
    ruleMapping.keywordFieldBuilder(FIELD_RULE_TEMPLATE_KEY).disableNorms().build();

    ruleMapping.keywordFieldBuilder(FIELD_RULE_TYPE).disableNorms().build();

    ruleMapping.createLongField(FIELD_RULE_CREATED_AT);
    ruleMapping.createLongField(FIELD_RULE_UPDATED_AT);

    ruleMapping.keywordFieldBuilder(FIELD_RULE_CWE).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_OWASP_TOP_10).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_OWASP_TOP_10_2021).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_SANS_TOP_25).disableNorms().build();
    ruleMapping.keywordFieldBuilder(FIELD_RULE_SONARSOURCE_SECURITY).disableNorms().build();

    ruleMapping.keywordFieldBuilder(FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY).disableNorms().build();
    ruleMapping.nestedFieldBuilder(FIELD_RULE_IMPACTS)
      .addKeywordField(SUB_FIELD_SOFTWARE_QUALITY)
      .addKeywordField(SUB_FIELD_SEVERITY)
      .build();

    // Active rule
    TypeMapping activeRuleMapping = index.createTypeMapping(TYPE_ACTIVE_RULE);
    activeRuleMapping.keywordFieldBuilder(FIELD_ACTIVE_RULE_UUID).disableNorms().build();
    activeRuleMapping.keywordFieldBuilder(FIELD_ACTIVE_RULE_PROFILE_UUID).disableNorms().build();
    activeRuleMapping.keywordFieldBuilder(FIELD_ACTIVE_RULE_INHERITANCE).disableNorms().build();
    activeRuleMapping.keywordFieldBuilder(FIELD_ACTIVE_RULE_SEVERITY).disableNorms().build();
    activeRuleMapping.createBooleanField(FIELD_PRIORITIZED_RULE);
    activeRuleMapping.nestedFieldBuilder(FIELD_ACTIVE_RULE_IMPACTS)
      .addKeywordField(SUB_FIELD_SOFTWARE_QUALITY)
      .addKeywordField(SUB_FIELD_SEVERITY).build()
    ;
  }
}
