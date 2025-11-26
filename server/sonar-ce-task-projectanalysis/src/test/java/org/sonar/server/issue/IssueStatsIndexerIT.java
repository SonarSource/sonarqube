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
import io.sonarcloud.compliancereports.ingestion.IssueIngestionService;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.report.IssueStatsByRuleKeyDaoImpl;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.issue.index.IssueIteratorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

class IssueStatsIndexerIT {
  @Rule
  public final DbTester db = DbTester.create();

  private final RuleDto rule1 = db.rules().insert(RuleTesting.randomRuleKey());
  private final RuleDto rule2 = db.rules().insert(RuleTesting.randomRuleKey());
  private final RuleDto rule3 = db.rules().insert(RuleTesting.randomRuleKey(), r -> r.setType(RuleType.SECURITY_HOTSPOT));

  private final IssueIteratorFactory issueIteratorFactory = new IssueIteratorFactory(db.getDbClient());
  private final DbClient dbClient = db.getDbClient();
  IssueIngestionService issueIngestionService = new IssueIngestionService(new IssueStatsByRuleKeyDaoImpl(dbClient));
  private final IssueStatsIndexer underTest = new IssueStatsIndexer(issueIteratorFactory, dbClient, issueIngestionService);

  @Nested
  class WhenIndexingOnAnalysis {

    @Test
    void shouldIngestAllIssuesFromAllProjectBranchesInApplication() {
      String applicationUuid = "application-uuid-1";
      String branchUuid1 = "appProjectBranch-1";
      String branchUuid2 = "appProjectBranch-2";
      String branchUuid3 = "appProjectBranch-3";
      String branchUuid4 = "appProjectBranch-4";
      var branches = List.of(
        insertProjectBranchWithIssues("appProject1", branchUuid1, Set.of(rule1)),
        insertProjectBranchWithIssues("appProject2", branchUuid2, Set.of(rule1, rule2)),
        insertProjectBranchWithIssues("appProject3", branchUuid3, Set.of(rule1, rule2)),
        insertProjectBranchWithIssues("appProject4", branchUuid4, Set.of(rule3))
      );
      insertApplicationWithProjectBranches(applicationUuid, branches);

      underTest.indexOnAnalysis(branchUuid1);
      underTest.indexOnAnalysis(branchUuid2);
      underTest.indexOnAnalysis(branchUuid3);
      underTest.indexOnAnalysis(branchUuid4);
      underTest.indexOnAnalysis(applicationUuid);

      var issueStats = new IssueStatsByRuleKeyDaoImpl(dbClient).getIssueStats(applicationUuid, AggregationType.APPLICATION);

      assertThat(issueStats)
        .hasSize(3)
        .extracting("ruleKey", "issueCount", "hotspotCount", "hotspotsReviewed")
        .containsExactlyInAnyOrder(
          tuple(rule1.getKey().toString(), 3, 0, 0),
          tuple(rule2.getKey().toString(), 2, 0, 0),
          tuple(rule3.getKey().toString(), 0, 1, 0)
        );
    }

    @Test
    void shouldIngestAllIssuesFromAllProjectBranchesInPortfolio() {
      String portfolioUuid = "portfolio-uuid-1";
      String branchUuid1 = "branch-1";
      String branchUuid2 = "branch-2";
      String branchUuid3 = "branch-3";
      String branchUuid4 = "branch-4";
      var branches = List.of(
        insertProjectBranchWithIssues("project1", branchUuid1, Set.of(rule1)),
        insertProjectBranchWithIssues("project2", branchUuid2, Set.of(rule1, rule2)),
        insertProjectBranchWithIssues("project3", branchUuid3, Set.of(rule1, rule2)),
        insertProjectBranchWithIssues("project4", branchUuid4, Set.of(rule3))
      );
      insertPortfolioWithProjectBranches(portfolioUuid, branches);

      underTest.indexOnAnalysis(branchUuid1);
      underTest.indexOnAnalysis(branchUuid2);
      underTest.indexOnAnalysis(branchUuid3);
      underTest.indexOnAnalysis(branchUuid4);
      underTest.indexOnAnalysis(portfolioUuid);

      var issueStats = new IssueStatsByRuleKeyDaoImpl(dbClient).getIssueStats(portfolioUuid, AggregationType.PORTFOLIO);

      assertThat(issueStats)
        .hasSize(3)
        .extracting("ruleKey", "issueCount", "hotspotCount", "hotspotsReviewed")
        .containsExactlyInAnyOrder(
          tuple(rule1.getKey().toString(), 3, 0, 0),
          tuple(rule2.getKey().toString(), 2, 0, 0),
          tuple(rule3.getKey().toString(), 0, 1, 0)
        );
    }

    @Test
    void shouldIngestAllIssuesOnProjectBranchForProject() {
      String branchUuid = "project-uuid-1";
      insertProjectBranchWithIssues("project-1", branchUuid, Set.of(rule1, rule2, rule3));

      underTest.indexOnAnalysis(branchUuid);

      var issueStats = new IssueStatsByRuleKeyDaoImpl(dbClient).getIssueStats(branchUuid, AggregationType.PROJECT);

      assertThat(issueStats)
        .hasSize(3)
        .extracting("ruleKey", "issueCount", "hotspotCount", "hotspotsReviewed")
        .containsExactlyInAnyOrder(
          tuple(rule1.getKey().toString(), 1, 0, 0),
          tuple(rule2.getKey().toString(), 1, 0, 0),
          tuple(rule3.getKey().toString(), 0, 1, 0)
        );
    }

    private void insertApplicationWithProjectBranches(String applicationUuid, List<BranchDto> branches) {
      db.components().insertPrivateApplication(applicationUuid);

      for (BranchDto branch : branches) {
        insertChildProject(applicationUuid, branch);
      }

      db.commit();
    }

    private void insertPortfolioWithProjectBranches(String portfolioUuid, List<BranchDto> branches) {
      var portfolio = new PortfolioDto()
        .setRootUuid(portfolioUuid)
        .setUuid(portfolioUuid)
        .setParentUuid(null)
        .setKee("my-portfolio")
        .setName("My Portfolio")
        .setSelectionMode(PortfolioDto.SelectionMode.MANUAL);
      var portfolioComponent = new ComponentDto()
        .setUuid(portfolioUuid)
        .setKey(portfolioUuid)
        .setUuidPath(".")
        .setBranchUuid(portfolioUuid)
        .setQualifier("VW");

      db.getDbClient().portfolioDao().insert(db.getSession(), portfolio, false);
      db.components().insertComponent(portfolioComponent);

      for (BranchDto branch : branches) {
        insertChildProject(portfolioUuid, branch);
      }

      db.commit();
    }

    private void insertChildProject(String applicationUuid, BranchDto branch) {
      db.components().insertComponent(new ComponentDto()
        .setBranchUuid(applicationUuid)
        .setKey(branch.getKey().substring(0, branch.getKey().length() - 5) + "_copy")
        .setUuid(branch.getUuid() + "_copy_uuid")
        .setUuidPath("." + applicationUuid + "." + branch.getUuid())
        .setCopyComponentUuid(branch.getUuid())
        .setQualifier("TRK")
        .setScope("PRJ"));
    }

    private BranchDto insertProjectBranchWithIssues(String projectUuid, String branchUuid, Set<RuleDto> rules) {
      var project = db.components().insertPrivateProject(projectUuid);
      var branch = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setUuid(branchUuid));
      var file = db.components().insertFile(branch);
      for (RuleDto rule : rules) {
        if (rule.getEnumType() == RuleType.SECURITY_HOTSPOT) {
          db.issues().insertHotspot(rule, branch, file);
        } else {
          db.issues().insert(rule, branch, file);
        }
      }
      db.commit();
      return branch;
    }
  }
}
