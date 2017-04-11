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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
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
import org.sonar.server.organization.DefaultOrganizationProvider;

import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE;

public class RuleIndexer implements StartupIndexer {

  private final EsClient esClient;
  private final DbClient dbClient;
  private final RuleIteratorFactory ruleIteratorFactory;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RuleIndexer(EsClient esClient, DbClient dbClient, RuleIteratorFactory ruleIteratorFactory, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.esClient = esClient;
    this.dbClient = dbClient;
    this.ruleIteratorFactory = ruleIteratorFactory;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_RULE);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    BulkIndexer bulk = new BulkIndexer(esClient, RuleIndexDefinition.INDEX).setSize(Size.LARGE);

    // index all definitions and system extensions
    if (uninitializedIndexTypes.contains(INDEX_TYPE_RULE)) {
      try(DbSession dbSession = dbClient.openSession(false)) {

        String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
        OrganizationDto defaultOrganization = dbClient.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid)
          .orElseThrow(() -> new IllegalStateException(String.format("Cannot load default organization for uuid '%s'", defaultOrganizationUuid)));

        try (RuleIterator rules = new RuleIteratorForSingleChunk(dbClient, defaultOrganization, null)) {
          doIndex(bulk, rules);
        }
      }
    }
  }

  public void index(OrganizationDto organization, RuleKey ruleKey) {
    BulkIndexer bulk = createBulkIndexer(Size.REGULAR);
    try (RuleIterator rules = ruleIteratorFactory.createForKey(organization, ruleKey)) {
      doIndex(bulk, rules);
    }
  }

  public void index(OrganizationDto organization, List<RuleKey> ruleKeys) {
    BulkIndexer bulk = createBulkIndexer(Size.REGULAR);
    try (RuleIterator rules = ruleIteratorFactory.createForKeys(organization, ruleKeys)) {
      doIndex(bulk, rules);
    }
  }

  @VisibleForTesting
  public void index(Iterator<RuleDoc> rules) {
    doIndex(createBulkIndexer(Size.REGULAR), rules);
  }

  private static void doIndex(BulkIndexer bulk, Iterator<RuleDoc> rules) {
    bulk.start();
    while (rules.hasNext()) {
      RuleDoc rule = rules.next();
      bulk.add(newIndexRequest(rule));
    }
    bulk.stop();
  }

  private BulkIndexer createBulkIndexer(Size size) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_RULE.getIndex());
    bulk.setSize(size);
    return bulk;
  }

  private static IndexRequest newIndexRequest(RuleDoc rule) {
    return new IndexRequest(INDEX_TYPE_RULE.getIndex(), INDEX_TYPE_RULE.getType(), rule.key().toString()).source(rule.getFields());
  }

  public void delete(RuleKey ruleKey) {
    esClient.prepareDelete(INDEX_TYPE_RULE, ruleKey.toString())
      .setRefresh(true)
      .get();
  }

  public void delete(List<RuleKey> rules) {
    BulkIndexer bulk = createBulkIndexer(Size.REGULAR);
    bulk.start();
    for (RuleKey rule : rules) {
      bulk.addDeletion(INDEX_TYPE_RULE, rule.toString());
    }
    bulk.stop();
  }
}
