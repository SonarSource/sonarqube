/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.startup;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.server.search.SearchIndex;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Fill search index with rules
 * @since 4.1
 */
public final class IndexRules {

  private static final String INDEX_RULES = "rules";
  private static final String TYPE_RULE = "rule";

  private SearchIndex searchIndex;
  private DatabaseSession session;
  private RuleI18nManager ruleI18nManager;

  public IndexRules(SearchIndex searchIndex, DatabaseSession session, RuleI18nManager ruleI18nManager) {
    this.searchIndex = searchIndex;
    this.session = session;
    this.ruleI18nManager = ruleI18nManager;
  }

  public void start() {

    try {
      searchIndex.addMappingFromClasspath(INDEX_RULES, TYPE_RULE, "/com/sonar/search/rule_mapping.json");
      List<String> ids = Lists.newArrayList();
      List<BytesStream> docs = Lists.newArrayList();
      for (Rule rule: session.getResults(Rule.class)) {
        ids.add(rule.getId().toString());
        XContentBuilder document = XContentFactory.jsonBuilder()
            .startObject()
            .field("id", rule.getId())
            .field("key", rule.ruleKey())
            .field("language", rule.getLanguage())
            .field("name", ruleI18nManager.getName(rule, Locale.getDefault()))
            .field("description", ruleI18nManager.getDescription(rule.getRepositoryKey(), rule.getKey(), Locale.getDefault()))
            .field("parentKey", rule.getParent() == null ? null : rule.getParent().getKey())
            .field("repositoryKey", rule.getRepositoryKey())
            .field("severity", rule.getSeverity())
            .field("status", rule.getStatus())
            .field("createdAt", rule.getCreatedAt())
            .field("updatedAt", rule.getUpdatedAt());
        if(!rule.getParams().isEmpty()) {
          document.startArray("params");
          for (RuleParam param: rule.getParams()) {
            document.startObject()
              .field("key", param.getKey())
              .field("type", param.getType())
              .field("defaultValue", param.getDefaultValue())
              .field("description", param.getDescription())
              .endObject();
          }
          document.endArray();
        }
        docs.add(document.endObject());
      }
      searchIndex.bulkIndex(INDEX_RULES, TYPE_RULE, ids.toArray(new String[0]), docs.toArray(new BytesStream[0]));
    } catch(IOException ioe) {
      throw new IllegalStateException("Unable to index rules", ioe);
    }
  }
}
