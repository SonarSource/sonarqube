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
package org.sonar.server.issue;

import io.sonarcloud.compliancereports.dao.AggregationType;
import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.ingestion.IssueFromAnalysis;
import io.sonarcloud.compliancereports.ingestion.IssueIngestionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.rule.Severity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.issue.IssueStatsDto;
import org.sonar.db.report.IssueStatsByRuleKeyDaoImpl;
import org.sonar.server.es.AnalysisIndexer;

import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;

public class IssueStatsIndexer implements AnalysisIndexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(IssueStatsIndexer.class);

  private final DbClient dbClient;
  private final IssueIngestionService issueIngestionService;

  public IssueStatsIndexer(DbClient dbClient, IssueIngestionService issueIngestionService) {
    this.dbClient = dbClient;
    this.issueIngestionService = issueIngestionService;
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    try (var dbSession = dbClient.openSession(false)) {
      EntityDto entity = dbClient.entityDao().selectByUuid(dbSession, branchUuid)
        .or(() -> dbClient.entityDao().selectByComponentUuid(dbSession, branchUuid))
        .orElseThrow(() -> new IllegalStateException("Can't find entity for uuid " + branchUuid));

      if (entity.isProject()) {
        ingestForSingleBranch(dbSession, branchUuid);
      } else {
        ingestForPortfolioOrApp(dbSession, branchUuid, entity.isPortfolio() ? AggregationType.PORTFOLIO : AggregationType.APPLICATION);
      }
    } catch (Exception e) {
      LOGGER.warn("Error ingesting issues for compliance reports", e);
    }
  }

  private void ingestForPortfolioOrApp(DbSession dbSession, String portfolioOrAppUuid, AggregationType aggregationType) {
    var projectBranchUuids = dbClient.componentDao().selectProjectBranchUuidsFromView(dbSession, portfolioOrAppUuid, portfolioOrAppUuid);
    var dao = new IssueStatsByRuleKeyDaoImpl(dbClient);
    Map<String, IssueStats> issueStatsByRuleKey = dao.loadAllIssueStatsForProjectBranches(projectBranchUuids).stream()
      .collect(Collectors.toMap(IssueStats::ruleKey, Function.identity(), IssueStatsIndexer::mergeIssueStats));
    dao.deleteAndInsertIssueStats(portfolioOrAppUuid, aggregationType, new ArrayList<>(issueStatsByRuleKey.values()));
    dbSession.commit();
  }

  private static IssueStats mergeIssueStats(IssueStats a, IssueStats b) {
    return new IssueStats(
      a.ruleKey(),
      a.issueCount() + b.issueCount(),
      Math.max(a.rating(), b.rating()),
      a.hotspotCount() + b.hotspotCount(),
      a.hotspotsReviewed() + b.hotspotsReviewed()
    );
  }

  private void ingestForSingleBranch(DbSession dbSession, String branchUuid) {
    List<IssueFromAnalysis> issuesForIngestion = new ArrayList<>();

    for (IssueStatsDto issue : dbClient.issueDao().scrollIssuesForIssueStats(dbSession, branchUuid)) {
      issuesForIngestion.add(toIssueFromAnalysis(issue));
    }

    issueIngestionService.ingest(branchUuid, AggregationType.PROJECT, issuesForIngestion);
    dbSession.commit();
  }

  private static IssueFromAnalysis toIssueFromAnalysis(IssueStatsDto issue) {
    String ruleKey = issue.getRepositoryKey() + ":" + issue.getRuleKey();
    boolean isHotspot = SECURITY_HOTSPOT.getDbConstant() == issue.getIssueType();
    // Adjust the 0-based (0-4) severity to 1-based (1-5) severity for the compliance module
    int severity = Severity.valueOf(issue.getSeverity()).ordinal() + 1;
    return new IssueFromAnalysis(ruleKey, issue.getStatus(), isHotspot, severity);
  }

}
