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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ReportSubscriptionDao implements Dao {

  public Optional<ReportSubscriptionDto> selectByUserAndPortfolio(DbSession dbSession, String portfolioUuid, String userUuid) {
    return mapper(dbSession).selectByUserAndPortfolio(portfolioUuid, userUuid);

  }

  public Optional<ReportSubscriptionDto> selectByUserAndBranch(DbSession dbSession, @Param("branchUuid") String branchUuid, @Param("userUuid") String userUuid) {
    return mapper(dbSession).selectByUserAndBranch(branchUuid, userUuid);

  }

  public List<ReportSubscriptionDto> selectByPortfolio(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectByPortfolio(portfolioUuid);
  }

  public List<ReportSubscriptionDto> selectByProjectBranch(DbSession dbSession, String branchUuid) {
    return mapper(dbSession).selectByBranch(branchUuid);
  }

  public Set<ReportSubscriptionDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public void delete(DbSession dbSession, ReportSubscriptionDto subscriptionDto) {
    mapper(dbSession).delete(subscriptionDto);
  }

  public void insert(DbSession dbSession, ReportSubscriptionDto subscriptionDto) {
    mapper(dbSession).insert(subscriptionDto);
  }

  private static ReportSubscriptionMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(ReportSubscriptionMapper.class);
  }

  public int countByQualifier(DbSession dbSession, String qualifier) {
    return mapper(dbSession).countByQualifier(qualifier);
  }

  public int countPortfolioReportSubscriptions(DbSession dbSession) {
    return mapper(dbSession).countPortfolioReportSubscriptions();
  }

  public Map<String, Integer> countPerProject(DbSession dbSession) {
    return mapper(dbSession).countPerProject().stream().collect(Collectors.toMap(SubscriptionCount::getProjectUuid, SubscriptionCount::getCount));
  }
}
