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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.StartupIndexer;

import static java.util.Collections.singletonList;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE_EXTENSION;

public class RuleIndexer implements StartupIndexer {

  private final EsClient esClient;
  private final DbClient dbClient;

  public RuleIndexer(EsClient esClient, DbClient dbClient) {
    this.esClient = esClient;
    this.dbClient = dbClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_RULE, INDEX_TYPE_RULE_EXTENSION);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    BulkIndexer bulk = new BulkIndexer(esClient, RuleIndexDefinition.INDEX, Size.LARGE);
    bulk.start();

    // index all definitions and system extensions
    if (uninitializedIndexTypes.contains(INDEX_TYPE_RULE)) {
      try (RuleIterator rules = new RuleIteratorForSingleChunk(dbClient, null)) {
        doIndexRuleDefinitions(rules, bulk);
      }
    }

    // index all organization extensions
    if (uninitializedIndexTypes.contains(INDEX_TYPE_RULE_EXTENSION)) {
      try (RuleMetadataIterator metadatas = new RuleMetadataIterator(dbClient)) {
        doIndexRuleExtensions(metadatas, bulk);
      }
    }

    bulk.stop();
  }

  public void indexRuleDefinition(RuleKey ruleKey) {
    indexRuleDefinitions(singletonList(ruleKey));
  }

  public void indexRuleDefinitions(List<RuleKey> ruleKeys) {
    BulkIndexer bulk = new BulkIndexer(esClient, RuleIndexDefinition.INDEX, Size.REGULAR);
    bulk.start();

    try (RuleIterator rules = new RuleIteratorForMultipleChunks(dbClient, ruleKeys)) {
      doIndexRuleDefinitions(rules, bulk);
    }

    bulk.stop();
  }

  public void indexRuleExtension(OrganizationDto organization, RuleKey ruleKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.ruleDao()
        .selectMetadataByKey(dbSession, ruleKey, organization)
        .map(ruleExtension -> RuleExtensionDoc.of(ruleKey, RuleExtensionScope.organization(organization), ruleExtension))
        .map(Arrays::asList)
        .map(List::iterator)
        .ifPresent(metadata -> {
          BulkIndexer bulk = new BulkIndexer(esClient, RuleIndexDefinition.INDEX, Size.REGULAR);
          bulk.start();
          doIndexRuleExtensions(metadata, bulk);
          bulk.stop();
        });
    }
  }

  private static void doIndexRuleDefinitions(Iterator<RuleDocWithSystemScope> rules, BulkIndexer bulk) {
    while (rules.hasNext()) {
      RuleDocWithSystemScope ruleWithExtension = rules.next();
      bulk.add(newIndexRequest(ruleWithExtension.getRuleDoc()));
      bulk.add(newIndexRequest(ruleWithExtension.getRuleExtensionDoc()));
    }
  }

  private static void doIndexRuleExtensions(Iterator<RuleExtensionDoc> metadatas, BulkIndexer bulk) {
    while (metadatas.hasNext()) {
      RuleExtensionDoc metadata = metadatas.next();
      bulk.add(newIndexRequest(metadata));
    }
  }

  private static IndexRequest newIndexRequest(RuleDoc rule) {
    return new IndexRequest(INDEX_TYPE_RULE.getIndex(), INDEX_TYPE_RULE.getType(), rule.key().toString()).source(rule.getFields());
  }

  private static IndexRequest newIndexRequest(RuleExtensionDoc ruleExtension) {
    return new IndexRequest(INDEX_TYPE_RULE_EXTENSION.getIndex(), INDEX_TYPE_RULE_EXTENSION.getType(), ruleExtension.getId())
      .source(ruleExtension.getFields())
      .parent(ruleExtension.getParent());
  }
}
