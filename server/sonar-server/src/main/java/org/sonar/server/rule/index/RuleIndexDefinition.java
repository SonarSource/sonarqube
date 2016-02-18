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
package org.sonar.server.rule.index;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

/**
 * Definition of ES index "rules", including settings and fields.
 */
public class RuleIndexDefinition implements IndexDefinition {

  public static final String INDEX = "rules";
  public static final String TYPE_RULE = "rule";

  public static final String FIELD_RULE_KEY = "key";

  // TODO find at what this field is useful ?
  public static final String FIELD_RULE_KEY_AS_LIST = "_key";

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

  public static final String FIELD_RULE_ALL_TAGS = "allTags";

  public static final String FIELD_RULE_CREATED_AT = "createdAt";
  public static final String FIELD_RULE_UPDATED_AT = "updatedAt";

  private final Settings settings;

  public RuleIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);

    index.refreshHandledByIndexer();
    index.setShards(settings);

    NewIndex.NewIndexType indexMapping = index.createType(TYPE_RULE);
    indexMapping.setAttribute("_id", ImmutableMap.of("path", FIELD_RULE_KEY));
    indexMapping.setAttribute("_parent", ImmutableMap.of("type", TYPE_RULE));
    indexMapping.setAttribute("_routing", ImmutableMap.of("required", true, "path", FIELD_RULE_REPOSITORY));
    indexMapping.setEnableSource(false);

    indexMapping.stringFieldBuilder(FIELD_RULE_KEY).enableSorting().enableGramSearch().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_KEY_AS_LIST).enableGramSearch().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_RULE_KEY).disableSearch().docValues().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_REPOSITORY).docValues().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_INTERNAL_KEY).disableSearch().docValues().build();

    indexMapping.stringFieldBuilder(FIELD_RULE_NAME).enableSorting().enableWordSearch().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_HTML_DESCRIPTION).enableWordSearch().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_SEVERITY).docValues().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_STATUS).docValues().build();
    indexMapping.stringFieldBuilder(FIELD_RULE_LANGUAGE).enableGramSearch().build();

    indexMapping.createBooleanField(FIELD_RULE_IS_TEMPLATE);
    indexMapping.stringFieldBuilder(FIELD_RULE_TEMPLATE_KEY).docValues().build();

    indexMapping.stringFieldBuilder(FIELD_RULE_ALL_TAGS).enableGramSearch().build();

    indexMapping.createLongField(FIELD_RULE_CREATED_AT);
    indexMapping.createLongField(FIELD_RULE_UPDATED_AT);
  }
}
