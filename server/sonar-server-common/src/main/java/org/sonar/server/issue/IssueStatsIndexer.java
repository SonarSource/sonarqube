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

import com.google.common.annotations.VisibleForTesting;
import io.sonarcloud.compliancereports.dao.AggregationType;
import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.ingestion.IssueFromAnalysis;
import io.sonarcloud.compliancereports.ingestion.IssueIngestionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.rule.Severity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.report.IssueStatsByRuleKeyDaoImpl;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.AnalysisIndexer;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIterator;
import org.sonar.server.issue.index.IssueIteratorFactory;

import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;

public class IssueStatsIndexer implements AnalysisIndexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(IssueStatsIndexer.class);

  private final IssueIteratorFactory issueIteratorFactory;
  private final DbClient dbClient;
  private final IssueIngestionService issueIngestionService;

  public IssueStatsIndexer(IssueIteratorFactory issueIteratorFactory, DbClient dbClient, IssueIngestionService issueIngestionService) {
    this.issueIteratorFactory = issueIteratorFactory;
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
    try (IssueIterator issues = issueIteratorFactory.createForBranch(branchUuid)) {
      List<IssueWithRuleUuidDto> issuesWithRuleUuids = getIssuesFromIssuesTable(issues);
      if (issuesWithRuleUuids.isEmpty()) {
        return;
      }

      var issuesForIngestion = transformToRepositoryRuleIssuesDtos(issuesWithRuleUuids, dbSession);
      issueIngestionService.ingest(branchUuid, AggregationType.PROJECT, issuesForIngestion);
      dbSession.commit();
    }
  }

  private static List<IssueWithRuleUuidDto> getIssuesFromIssuesTable(IssueIterator issues) {
    List<IssueWithRuleUuidDto> issueWithRuleUuidDtos = new ArrayList<>();
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      issueWithRuleUuidDtos.add(getIssueWithRuleDto(issue));
    }
    return issueWithRuleUuidDtos;
  }

  @VisibleForTesting
  List<IssueFromAnalysis> transformToRepositoryRuleIssuesDtos(List<IssueWithRuleUuidDto> issueWithRuleUuidDtos, DbSession dbSession) {
    Set<String> ruleUuids = issueWithRuleUuidDtos.stream().map(IssueWithRuleUuidDto::ruleUuid).collect(Collectors.toSet());
    Map<String, String> ruleUuidToFullRepositoryRuleKey = getRuleUuidToFullRepositoryRuleKey(dbSession, ruleUuids);
    return issueWithRuleUuidDtos.stream()
      .filter(dto -> ruleUuidToFullRepositoryRuleKey.containsKey(dto.ruleUuid()))
      .map(issueWithRuleUuidDto -> new IssueFromAnalysis(ruleUuidToFullRepositoryRuleKey.get(issueWithRuleUuidDto.ruleUuid()),
        issueWithRuleUuidDto.status(),
        issueWithRuleUuidDto.isHotspot(),
        issueWithRuleUuidDto.severity()))
      .toList();
  }

  private Map<String, String> getRuleUuidToFullRepositoryRuleKey(DbSession dbSession, Set<String> ruleUuids) {
    return dbClient.ruleDao().selectByUuids(dbSession, ruleUuids).stream().collect(Collectors.toMap(
      RuleDto::getUuid,
      dto -> dto.getRepositoryKey() + ":" + dto.getRuleKey()
    ));
  }

  private static IssueWithRuleUuidDto getIssueWithRuleDto(IssueDoc issue) {
    String ruleUuid = issue.ruleUuid();
    String status = issue.status();
    boolean isHotspot = SECURITY_HOTSPOT.equals(issue.type());
    int severity = Severity.valueOf(issue.severity()).ordinal();
    return new IssueWithRuleUuidDto(ruleUuid, status, isHotspot, severity);
  }

  @VisibleForTesting
  record IssueWithRuleUuidDto(String ruleUuid, String status, boolean isHotspot, int severity) {
  }
}
