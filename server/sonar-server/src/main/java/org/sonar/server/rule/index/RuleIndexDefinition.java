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
package org.sonar.server.rule.index;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Set;
import org.sonar.api.config.Configuration;
import org.sonar.server.es.DefaultIndexSettings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.NewIndex;

import static org.sonar.server.es.DefaultIndexSettingsElement.ENGLISH_HTML_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_WORDS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;

/**
 * Definition of ES index "rules", including settings and fields.
 */
public class RuleIndexDefinition implements IndexDefinition {

  static final String INDEX = "rules";

  public static final IndexType INDEX_TYPE_RULE = new IndexType(INDEX, "rule");
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
  public static final String FIELD_RULE_TEMPLATE_KEY = "templateKey";
  public static final String FIELD_RULE_TYPE = "type";
  public static final String FIELD_RULE_CREATED_AT = "createdAt";
  public static final String FIELD_RULE_UPDATED_AT = "updatedAt";

  public static final Set<String> SORT_FIELDS = ImmutableSet.of(
    FIELD_RULE_NAME,
    FIELD_RULE_UPDATED_AT,
    FIELD_RULE_CREATED_AT,
    FIELD_RULE_KEY);

  // Rule extension fields
  public static final IndexType INDEX_TYPE_RULE_EXTENSION = new IndexType(INDEX, "ruleExtension");
  /** The uuid of a {@link RuleExtensionScope} */
  public static final String FIELD_RULE_EXTENSION_SCOPE = "scope";
  public static final String FIELD_RULE_EXTENSION_RULE_KEY = "ruleKey";
  public static final String FIELD_RULE_EXTENSION_TAGS = "tags";

  // Active rule fields
  public static final IndexType INDEX_TYPE_ACTIVE_RULE = new IndexType(INDEX, "activeRule");
  public static final String FIELD_ACTIVE_RULE_REPOSITORY = "repo";
  public static final String FIELD_ACTIVE_RULE_INHERITANCE = "inheritance";
  public static final String FIELD_ACTIVE_RULE_PROFILE_UUID = "ruleProfile";
  public static final String FIELD_ACTIVE_RULE_SEVERITY = "severity";
  public static final String FIELD_ACTIVE_RULE_RULE_KEY = "ruleKey";

  private final Configuration config;
  private final boolean enableSource;

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
  public static RuleIndexDefinition createForTest(Configuration config) {
    return new RuleIndexDefinition(config, true);
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_TYPE_RULE.getIndex());

    index.refreshHandledByIndexer();
    // Default nb of shards should be greater than 1 in order to
    // easily detect routing misconfiguration.
    // See https://jira.sonarsource.com/browse/SONAR-9489
    index.configureShards(config, 2);

    // Active rule type
    NewIndex.NewIndexType activeRuleMapping = index.createType(INDEX_TYPE_ACTIVE_RULE.getType());
    activeRuleMapping.setEnableSource(enableSource);
    activeRuleMapping.setAttribute("_parent", ImmutableMap.of("type", INDEX_TYPE_RULE.getType()));

    activeRuleMapping.stringFieldBuilder(FIELD_ACTIVE_RULE_RULE_KEY).addSubFields(SORTABLE_ANALYZER).build();
    activeRuleMapping.stringFieldBuilder(FIELD_ACTIVE_RULE_REPOSITORY).build();
    activeRuleMapping.stringFieldBuilder(FIELD_ACTIVE_RULE_PROFILE_UUID).disableNorms().build();
    activeRuleMapping.stringFieldBuilder(FIELD_ACTIVE_RULE_INHERITANCE).disableNorms().build();
    activeRuleMapping.stringFieldBuilder(FIELD_ACTIVE_RULE_SEVERITY).disableNorms().build();

    // Rule extension type
    NewIndex.NewIndexType ruleExtensionType = index.createType(INDEX_TYPE_RULE_EXTENSION.getType());
    ruleExtensionType.setEnableSource(enableSource);
    ruleExtensionType.setAttribute("_parent", ImmutableMap.of("type", INDEX_TYPE_RULE.getType()));

    ruleExtensionType.stringFieldBuilder(FIELD_RULE_EXTENSION_SCOPE).disableNorms().build();
    ruleExtensionType.stringFieldBuilder(FIELD_RULE_EXTENSION_RULE_KEY).disableNorms().build();
    ruleExtensionType.stringFieldBuilder(FIELD_RULE_EXTENSION_TAGS).build();

    // Rule type
    NewIndex.NewIndexType ruleMapping = index.createType(INDEX_TYPE_RULE.getType());
    ruleMapping.setEnableSource(enableSource);

    ruleMapping.stringFieldBuilder(FIELD_RULE_KEY).addSubFields(SORTABLE_ANALYZER).build();
    ruleMapping.stringFieldBuilder(FIELD_RULE_RULE_KEY).addSubFields(SORTABLE_ANALYZER).build();
    ruleMapping.stringFieldBuilder(FIELD_RULE_REPOSITORY).build();
    ruleMapping.stringFieldBuilder(FIELD_RULE_INTERNAL_KEY).disableNorms().disableSearch().build();

    ruleMapping.stringFieldBuilder(FIELD_RULE_NAME).addSubFields(SORTABLE_ANALYZER, SEARCH_WORDS_ANALYZER).build();
    ruleMapping.setProperty(FIELD_RULE_HTML_DESCRIPTION, ImmutableSortedMap.of(
      DefaultIndexSettings.TYPE, DefaultIndexSettings.STRING,
      DefaultIndexSettings.INDEX, DefaultIndexSettings.ANALYZED,
      DefaultIndexSettings.ANALYZER, ENGLISH_HTML_ANALYZER.getName(),
      DefaultIndexSettings.SEARCH_ANALYZER, ENGLISH_HTML_ANALYZER.getName()));
    ruleMapping.stringFieldBuilder(FIELD_RULE_SEVERITY).disableNorms().build();
    ruleMapping.stringFieldBuilder(FIELD_RULE_STATUS).disableNorms().build();
    ruleMapping.stringFieldBuilder(FIELD_RULE_LANGUAGE).disableNorms().build();

    ruleMapping.createBooleanField(FIELD_RULE_IS_TEMPLATE);
    ruleMapping.stringFieldBuilder(FIELD_RULE_TEMPLATE_KEY).disableNorms().build();

    ruleMapping.stringFieldBuilder(FIELD_RULE_TYPE).disableNorms().build();

    ruleMapping.createLongField(FIELD_RULE_CREATED_AT);
    ruleMapping.createLongField(FIELD_RULE_UPDATED_AT);
  }
}
