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

import java.util.Iterator;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_UPDATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE;

public class RuleIndexer extends BaseIndexer {

  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RuleIndexer(System2 system2, DbClient dbClient, EsClient esClient,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    super(system2, esClient, 300, INDEX_TYPE_RULE, FIELD_RULE_UPDATED_AT);
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(Size.REGULAR), lastUpdatedAt);
  }

  public void index(Iterator<RuleDoc> rules) {
    doIndex(createBulkIndexer(Size.REGULAR), rules);
  }

  private long doIndex(BulkIndexer bulk, long lastUpdatedAt) {
    long maxDate;
    try (DbSession dbSession = dbClient.openSession(false)) {
      String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
      RuleResultSetIterator rowIt = RuleResultSetIterator.create(dbClient, dbSession, defaultOrganizationUuid, lastUpdatedAt);
      maxDate = doIndex(bulk, rowIt);
      rowIt.close();
      return maxDate;
    }
  }

  private static long doIndex(BulkIndexer bulk, Iterator<RuleDoc> rules) {
    bulk.start();
    long maxDate = 0L;
    while (rules.hasNext()) {
      RuleDoc rule = rules.next();
      bulk.add(newIndexRequest(rule));

      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      maxDate = Math.max(maxDate, rule.updatedAt());
    }
    bulk.stop();
    return maxDate;
  }

  private BulkIndexer createBulkIndexer(Size size) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_RULE.getIndex());
    bulk.setSize(size);
    return bulk;
  }

  private static IndexRequest newIndexRequest(RuleDoc rule) {
    return new IndexRequest(INDEX_TYPE_RULE.getIndex(), INDEX_TYPE_RULE.getType(), rule.key().toString()).source(rule.getFields());
  }
}
