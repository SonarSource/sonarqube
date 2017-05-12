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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.rule.index.RuleIndexDefinition;

import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE;

public class ActiveRuleIndexer implements StartupIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;

  public ActiveRuleIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE);
  }

  @Override
  public void indexOnStartup(Set<IndexType> emptyIndexTypes) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_ACTIVE_RULE.getIndex(), Size.LARGE);
    try (DbSession dbSession = dbClient.openSession(false);
      ActiveRuleResultSetIterator rowIt = ActiveRuleResultSetIterator.create(dbClient, dbSession)) {
      doIndex(bulk, rowIt);
    }
  }

  public void index(List<ActiveRuleChange> changes) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_ACTIVE_RULE.getIndex(), Size.REGULAR);
    bulk.start();
    changes.stream()
      .map(c -> {
        switch (c.getType()) {
          case ACTIVATED:
          case UPDATED:
            return newIndexRequest(changeToDoc(c));
          case DEACTIVATED:
            return newDeleteRequest(c);
          default:
            throw new IllegalStateException("Unexpected change type " + c.getType());
        }
      })
      .forEach(bulk::add);
    bulk.stop();
  }

  public ActiveRuleDoc changeToDoc(ActiveRuleChange c) {
    ActiveRuleDoc doc = new ActiveRuleDoc(c.getKey())
      .setSeverity(c.getSeverity())
      .setOrganizationUuid(c.getOrganizationUuid());
    ActiveRule.Inheritance inheritance = c.getInheritance();
    if (inheritance != null) {
      doc.setInheritance(inheritance.name());
    }
    return doc;
  }

  public void index(Iterator<ActiveRuleDoc> rules) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_ACTIVE_RULE.getIndex(), Size.REGULAR);
    doIndex(bulk, rules);
  }

  private static void doIndex(BulkIndexer bulk, Iterator<ActiveRuleDoc> activeRules) {
    bulk.start();
    while (activeRules.hasNext()) {
      ActiveRuleDoc activeRule = activeRules.next();
      bulk.add(newIndexRequest(activeRule));
    }
    bulk.stop();
  }

  private static IndexRequest newIndexRequest(ActiveRuleDoc doc) {
    return new IndexRequest(INDEX_TYPE_ACTIVE_RULE.getIndex(), INDEX_TYPE_ACTIVE_RULE.getType(), doc.key().toString())
      .parent(doc.key().ruleKey().toString())
      .source(doc.getFields());
  }

  public DeleteRequest newDeleteRequest(ActiveRuleChange c) {
    return esClient.prepareDelete(INDEX_TYPE_ACTIVE_RULE, c.getKey().toString())
      .setParent(c.getKey().ruleKey().toString())
      .request();
  }

  public void deleteByProfileKeys(Collection<String> profileKeys) {
    BulkIndexer.delete(esClient, INDEX_TYPE_ACTIVE_RULE.getIndex(), esClient.prepareSearch(INDEX_TYPE_ACTIVE_RULE)
      .setQuery(termsQuery(FIELD_ACTIVE_RULE_PROFILE_KEY, profileKeys)));
  }
}
