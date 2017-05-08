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
package org.sonar.server.qualityprofile.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_UPDATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE;

public class ActiveRuleIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public ActiveRuleIndexer(System2 system2, DbClient dbClient, EsClient esClient) {
    super(system2, esClient, 300, INDEX_TYPE_ACTIVE_RULE, FIELD_ACTIVE_RULE_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(), lastUpdatedAt);
  }

  public void index(Iterator<ActiveRuleDoc> rules) {
    doIndex(createBulkIndexer(), rules);
  }

  private long doIndex(BulkIndexer bulk, long lastUpdatedAt) {
    long maxDate;
    try (DbSession dbSession = dbClient.openSession(false)) {
      ActiveRuleResultSetIterator rowIt = ActiveRuleResultSetIterator.create(dbClient, dbSession, lastUpdatedAt);
      maxDate = doIndex(bulk, rowIt);
      rowIt.close();
      return maxDate;
    }
  }

  private static long doIndex(BulkIndexer bulk, Iterator<ActiveRuleDoc> activeRules) {
    bulk.start();
    long maxDate = 0L;
    while (activeRules.hasNext()) {
      ActiveRuleDoc activeRule = activeRules.next();
      bulk.add(newIndexRequest(activeRule));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      maxDate = Math.max(maxDate, activeRule.updatedAt());
    }
    bulk.stop();
    return maxDate;
  }

  public void index(List<ActiveRuleChange> changes) {
    deleteKeys(changes.stream()
      .filter(c -> c.getType().equals(ActiveRuleChange.Type.DEACTIVATED))
      .map(ActiveRuleChange::getKey)
      .collect(MoreCollectors.toList(changes.size())));

    index();
  }

  public void deleteByProfileKeys(Collection<String> profileKeys) {
    BulkIndexer bulk = createBulkIndexer();
    bulk.start();
    profileKeys.forEach(profileKey -> {
      SearchRequestBuilder search = esClient.prepareSearch(INDEX_TYPE_ACTIVE_RULE)
        .setQuery(QueryBuilders.boolQuery().must(termsQuery(FIELD_ACTIVE_RULE_PROFILE_KEY, profileKey)));
      bulk.addDeletion(search);
    });
    bulk.stop();
  }

  private void deleteKeys(List<ActiveRuleKey> keys) {
    BulkIndexer bulk = createBulkIndexer();
    bulk.start();
    SearchRequestBuilder search = esClient.prepareSearch(INDEX_TYPE_ACTIVE_RULE)
      .setQuery(QueryBuilders.boolQuery().must(termsQuery(FIELD_ACTIVE_RULE_KEY, keys)));
    bulk.addDeletion(search);
    bulk.stop();
  }

  private BulkIndexer createBulkIndexer() {
    return new BulkIndexer(esClient, INDEX_TYPE_ACTIVE_RULE.getIndex(), Size.REGULAR);
  }

  private static IndexRequest newIndexRequest(ActiveRuleDoc doc) {
    return new IndexRequest(INDEX_TYPE_ACTIVE_RULE.getIndex(), INDEX_TYPE_ACTIVE_RULE.getType(), doc.key().toString())
      .parent(doc.key().ruleKey().toString())
      .source(doc.getFields());
  }
}
