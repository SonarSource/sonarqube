/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.report;

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonarsource.compliancereports.dao.AggregationType;
import org.sonarsource.compliancereports.dao.IssueStats;
import org.sonarsource.compliancereports.dao.IssueStatsByRuleKeyDao;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class IssueStatsByRuleKeyDaoImpl implements IssueStatsByRuleKeyDao {

  private final DbClient dbClient;

  public IssueStatsByRuleKeyDaoImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public List<IssueStats> getIssueStats(String uuid, AggregationType aggregationType) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return mapper(dbSession).selectByAggregationId(uuid, aggregationType.toString());
    }
  }

  public List<IssueStats> loadAllIssueStatsForProjectBranches(List<String> branchUuids) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return executeLargeInputs(branchUuids, input -> mapper(dbSession).selectByAggregationIds(input,
        AggregationType.PROJECT.toString()));
    }
  }

  @Override
  public void deleteAndInsertIssueStats(String aggregationId, AggregationType aggregationType, List<IssueStats> list) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      mapper(dbSession).deleteAllIssueStats(aggregationId, aggregationType.toString());
      for (IssueStats i : list) {
        mapper(dbSession).insertIssueStats(aggregationId, aggregationType.toString(), i);
      }
      dbSession.commit();
    }
  }

  @Override
  public void deleteByAggregationAndRuleKey(String aggregationId, AggregationType aggregationType, String ruleKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      mapper(dbSession).deleteIssueStatsForAggregationAndRuleKey(aggregationId, aggregationType.toString(), ruleKey);
      dbSession.commit();
    }
  }

  @Override
  public void upsert(String aggregationId, AggregationType aggregationType, IssueStats issueStats) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      mapper(dbSession).deleteIssueStatsForAggregationAndRuleKey(aggregationId, aggregationType.toString(), issueStats.ruleKey());
      mapper(dbSession).insertIssueStats(aggregationId, aggregationType.toString(), issueStats);
      dbSession.commit();
    }
  }

  @Override
  public IssueStats aggregateIssueStatsForBranchUuidAndRuleKey(String aggregationId, String ruleKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return mapper(dbSession).getIssueStatsByAggregationIdAndRuleKey(aggregationId, ruleKey);
    }
  }

  private static IssueStatsByRuleKeyMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(IssueStatsByRuleKeyMapper.class);
  }
}
