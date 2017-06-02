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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.rule.index.RuleIndexDefinition;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE;

public class ActiveRuleIndexer implements StartupIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;
  private final ActiveRuleIteratorFactory activeRuleIteratorFactory;

  public ActiveRuleIndexer(DbClient dbClient, EsClient esClient, ActiveRuleIteratorFactory activeRuleIteratorFactory) {
    this.dbClient = dbClient;
    this.esClient = esClient;
    this.activeRuleIteratorFactory = activeRuleIteratorFactory;
  }

  @Override
  public void indexOnStartup(Set<IndexType> emptyIndexTypes) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ActiveRuleIterator dbCursor = activeRuleIteratorFactory.createForAll(dbSession);
      scrollDbAndIndex(dbCursor, Size.LARGE);
    }
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE);
  }

  /**
   * Important - the existing documents are not deleted, so this method
   * does not guarantee consistency of index.
   */
  public void indexRuleProfile(DbSession dbSession, RulesProfileDto ruleProfile) {
    try (ActiveRuleIterator dbCursor = activeRuleIteratorFactory.createForRuleProfile(dbSession, ruleProfile)) {
      scrollDbAndIndex(dbCursor, Size.REGULAR);
    }
  }

  public void indexChanges(DbSession dbSession, List<ActiveRuleChange> changes) {
    BulkIndexer bulk = createBulkIndexer(Size.REGULAR);
    bulk.start();
    List<Integer> idsOfTouchedActiveRules = new ArrayList<>();
    changes.stream()
      .filter(c -> c.getActiveRule() != null)
      .forEach(c -> {
        if (c.getType().equals(ActiveRuleChange.Type.DEACTIVATED)) {
          bulk.addDeletion(INDEX_TYPE_ACTIVE_RULE, String.valueOf(c.getActiveRule().getId()));
        } else {
          idsOfTouchedActiveRules.add(c.getActiveRule().getId());
        }
      });
    try (ActiveRuleIterator dbCursor = activeRuleIteratorFactory.createForActiveRules(dbSession, idsOfTouchedActiveRules)) {
      while (dbCursor.hasNext()) {
        ActiveRuleDoc activeRule = dbCursor.next();
        bulk.add(newIndexRequest(activeRule));
      }
    }
    bulk.stop();
  }

  public void deleteByProfiles(Collection<QProfileDto> profiles) {
    BulkIndexer bulk = createBulkIndexer(Size.REGULAR);
    bulk.start();
    profiles.forEach(profile -> {
      SearchRequestBuilder search = esClient.prepareSearch(INDEX_TYPE_ACTIVE_RULE)
        .setQuery(QueryBuilders.boolQuery().must(termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid())));
      bulk.addDeletion(search);
    });
    bulk.stop();
  }

  public void deleteByRuleKeys(Collection<RuleKey> ruleKeys) {
    BulkIndexer bulk = createBulkIndexer(Size.REGULAR);
    bulk.start();
    ruleKeys.forEach(ruleKey -> {
      SearchRequestBuilder search = esClient.prepareSearch(INDEX_TYPE_ACTIVE_RULE)
        .setQuery(QueryBuilders.boolQuery().must(termQuery(FIELD_ACTIVE_RULE_RULE_KEY, ruleKey.toString())));
      bulk.addDeletion(search);
    });
    bulk.stop();
  }

  private BulkIndexer createBulkIndexer(Size size) {
    return new BulkIndexer(esClient, INDEX_TYPE_ACTIVE_RULE.getIndex(), size);
  }

  private static IndexRequest newIndexRequest(ActiveRuleDoc doc) {
    return new IndexRequest(INDEX_TYPE_ACTIVE_RULE.getIndex(), INDEX_TYPE_ACTIVE_RULE.getType(), doc.getId())
      .parent(doc.getRuleKey().toString())
      .source(doc.getFields());
  }

  private void scrollDbAndIndex(ActiveRuleIterator dbCursor, Size size) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_ACTIVE_RULE.getIndex(), size);
    bulk.start();
    while (dbCursor.hasNext()) {
      ActiveRuleDoc activeRule = dbCursor.next();
      bulk.add(newIndexRequest(activeRule));
    }
    bulk.stop();
  }
}
