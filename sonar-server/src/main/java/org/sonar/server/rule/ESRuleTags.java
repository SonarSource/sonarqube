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
package org.sonar.server.rule;

import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.es.ESIndex;

import java.io.IOException;
import java.util.Collection;

public class ESRuleTags {

  public static final String TYPE_TAG = "tag";

  private final ESIndex index;

  public ESRuleTags(ESIndex index) {
    this.index = index;
  }

  public void start() {
    index.addMappingFromClasspath(RuleRegistry.INDEX_RULES, TYPE_TAG, "/org/sonar/server/es/config/mappings/tag_mapping.json");
  }

  public void putAllTags(Collection<RuleTagDto> tags) {
    String[] ids = new String[tags.size()];
    try {
      if (!tags.isEmpty()) {
        BytesStream[] sources = new BytesStream[tags.size()];
        int tagCounter = 0;
        for (RuleTagDto tag: tags) {
          ids[tagCounter] = tag.getTag();
          sources[tagCounter] = ruleTagDocument(tag);
          tagCounter ++;
        }
        index.bulkIndex(RuleRegistry.INDEX_RULES, TYPE_TAG, ids, sources);
      }
      index.client().prepareDeleteByQuery(RuleRegistry.INDEX_RULES).setTypes(TYPE_TAG)
      .setQuery(
          QueryBuilders.filteredQuery(
              QueryBuilders.matchAllQuery(),
              FilterBuilders.notFilter(
                  FilterBuilders.termsFilter(RuleTagDocument.FIELD_VALUE, ids))))
                  .execute().actionGet();
    } catch(IOException ioException) {
      throw new IllegalStateException("Unable to index tags", ioException);
    }
  }

  public void put(RuleTagDto tag) {
    try {
      index.putSynchronous(RuleRegistry.INDEX_RULES, TYPE_TAG, tag.getTag(), ruleTagDocument(tag));
    } catch (IOException ioException) {
      throw new IllegalStateException("Unable to index tag "+tag.getTag(), ioException);
    }
  }

  public Collection<String> searchAllTags() {
    final int scrollSize = 50;
    return index.findDocumentIds(index.client().prepareSearch(RuleRegistry.INDEX_RULES)
      .setTypes(TYPE_TAG)
      .addSort(RuleTagDocument.FIELD_VALUE, SortOrder.ASC), scrollSize);
  }

  private XContentBuilder ruleTagDocument(RuleTagDto tag) throws IOException {
    return XContentFactory.jsonBuilder()
      .startObject()
      .field(RuleTagDocument.FIELD_VALUE, tag.getTag())
      .endObject();
  }

  public void delete(String... deleted) {
    index.bulkDelete(RuleRegistry.INDEX_RULES, TYPE_TAG, deleted);
  }
}
