/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import io.sonarcloud.compliancereports.dao.AggregationType;
import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import java.util.List;
import java.util.UUID;
import org.sonar.db.DbSession;

public class IssueStatsByRuleKeyDaoImpl implements IssueStatsByRuleKeyDao {

  private final DbSession dbSession;

  public IssueStatsByRuleKeyDaoImpl(DbSession dbSession) {
    this.dbSession = dbSession;
  }

  @Override
  public List<IssueStats> getIssueStats(String uuid, AggregationType aggregationType) {
    return mapper(dbSession).selectByAggregationId(String.valueOf(uuid));
  }

  @Override
  public void insertIssueStats(String aggregationId, AggregationType aggregationType, List<IssueStats> list) {
    mapper(dbSession).insertIssueStatsForProject(String.valueOf(aggregationId), list);
  }

  @Override
  public void deleteAllIssueStats(String uuid, AggregationType aggregationType) {
    mapper(dbSession).deleteAllIssueStatsForProject(String.valueOf(uuid));
  }

  private static IssueStatsByRuleKeyMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(IssueStatsByRuleKeyMapper.class);
  }
}
