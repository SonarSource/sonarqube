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
package org.sonar.server.qualityprofile.index;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.rule.index.RuleIndexDefinition;

import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_UPDATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;

public class ActiveRuleIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public ActiveRuleIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 300, INDEX, TYPE_ACTIVE_RULE, FIELD_ACTIVE_RULE_UPDATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(false), lastUpdatedAt);
  }

  public void index(Iterator<ActiveRuleDoc> rules) {
    doIndex(createBulkIndexer(false), rules);
  }

  private long doIndex(BulkIndexer bulk, long lastUpdatedAt) {
    DbSession dbSession = dbClient.openSession(false);
    long maxDate;
    try {
      ActiveRuleResultSetIterator rowIt = ActiveRuleResultSetIterator.create(dbClient, dbSession, lastUpdatedAt);
      maxDate = doIndex(bulk, rowIt);
      rowIt.close();
      return maxDate;
    } finally {
      dbSession.close();
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
    deleteKeys(FluentIterable.from(changes)
      .filter(MatchDeactivatedRule.INSTANCE)
      .transform(ActiveRuleChangeToKey.INSTANCE)
      .toList());
    index();
  }

  public void deleteProfile(String qualityProfileKey) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    bulk.start();
    SearchRequestBuilder search = esClient.prepareSearch(INDEX)
      .setTypes(TYPE_ACTIVE_RULE)
      .setQuery(QueryBuilders.filteredQuery(
        QueryBuilders.matchAllQuery(),
        FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_KEY, qualityProfileKey))
        ));
    bulk.addDeletion(search);
    bulk.stop();
  }

  private void deleteKeys(List<ActiveRuleKey> keys) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    bulk.start();
    SearchRequestBuilder search = esClient.prepareSearch(INDEX)
      .setTypes(TYPE_ACTIVE_RULE)
      .setQuery(QueryBuilders.filteredQuery(
        QueryBuilders.matchAllQuery(),
        FilterBuilders.boolFilter().must(FilterBuilders.termsFilter(RuleIndexDefinition.FIELD_ACTIVE_RULE_KEY, keys))
        ));
    bulk.addDeletion(search);
    bulk.stop();
  }

  private BulkIndexer createBulkIndexer(boolean large) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX);
    bulk.setLarge(large);
    return bulk;
  }

  private static IndexRequest newIndexRequest(ActiveRuleDoc doc) {
    return new IndexRequest(INDEX, TYPE_ACTIVE_RULE, doc.key().toString())
      .parent(doc.key().ruleKey().toString())
      .routing(doc.key().ruleKey().repository())
      .source(doc.getFields());
  }

  private enum MatchDeactivatedRule implements Predicate<ActiveRuleChange> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull ActiveRuleChange input) {
      return input.getType().equals(ActiveRuleChange.Type.DEACTIVATED);
    }
  }

  private enum ActiveRuleChangeToKey implements Function<ActiveRuleChange, ActiveRuleKey> {
    INSTANCE;

    @Override
    public ActiveRuleKey apply(@Nonnull ActiveRuleChange input) {
      return input.getKey();
    }
  }

}
