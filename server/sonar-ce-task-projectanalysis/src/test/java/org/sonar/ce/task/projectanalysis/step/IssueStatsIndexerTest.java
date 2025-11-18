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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.report.IssueStatsByRuleKeyMapper;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIterator;
import org.sonar.server.issue.index.IssueIteratorFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class IssueStatsIndexerTest {


  private static final String RULE_UUID = "AYo-aF5iSKQvrzz8Va0j";
  private static final String BRANCH_UUID = "7cf2e718-08d2-40c9-85e8-533665bb2397";

  private final IssueIteratorFactory issueIteratorFactory = mock(IssueIteratorFactory.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final IssueStatsIndexer underTest = new IssueStatsIndexer(issueIteratorFactory, dbClient);

  @Nested
  class WhenIndexingOnAnalysis {

    @Test
    void shouldNotOpenDbSessionWhenNoIssues() {
      IssueIterator issueIterator = mock(IssueIterator.class);
      when(issueIteratorFactory.createForBranch(BRANCH_UUID)).thenReturn(issueIterator);
      when(issueIterator.hasNext()).thenReturn(false);

      underTest.indexOnAnalysis(BRANCH_UUID);

      verify(dbClient, never()).openSession(anyBoolean());
    }

    @Test
    void shouldOpenDbSessionWhenIssuesExist() {
      IssueIterator issueIterator = mock(IssueIterator.class);
      when(issueIteratorFactory.createForBranch(BRANCH_UUID)).thenReturn(issueIterator);
      when(issueIterator.hasNext()).thenReturn(true, false);
      IssueDoc issueDoc = getIssueDoc(RuleType.BUG);
      when(issueIterator.next()).thenReturn(issueDoc);

      var dbSession = mock(DbSession.class);
      when(dbClient.openSession(false)).thenReturn(dbSession);
      RuleDto ruleDto = getRuleDto();
      RuleDao ruleDao = getRuleDao();
      when(ruleDao.selectByUuids(dbSession, Set.of(RULE_UUID))).thenReturn(List.of(ruleDto));
      when(dbSession.getMapper(IssueStatsByRuleKeyMapper.class)).thenReturn(mock(IssueStatsByRuleKeyMapper.class));

      underTest.indexOnAnalysis(BRANCH_UUID);

      verify(dbClient).openSession(false);
      verify(dbSession).commit();
    }

    @Test
    void shouldTransformIssueDocToIssueFromAnalysis() {
      IssueDoc issueDoc = getIssueDoc(RuleType.SECURITY_HOTSPOT);

      var dbSession = mock(DbSession.class);
      RuleDto ruleDto = getRuleDto();
      RuleDao ruleDao = getRuleDao();
      when(ruleDao.selectByUuids(dbSession, Set.of(RULE_UUID))).thenReturn(List.of(ruleDto));

      var issueWithRuleUuidDtos = getIssueWithRuleUuidDtos(issueDoc);
      var issuesForIngestion = underTest.transformToRepositoryRuleIssuesDtos(issueWithRuleUuidDtos, dbSession);

      assertThat(issuesForIngestion).hasSize(1);
      var issueForIngestion = issuesForIngestion.get(0);
      assertThat(issueForIngestion.ruleKey()).isEqualTo("REPO_KEY:RULE_KEY");
      assertThat(issueForIngestion.status()).isEqualTo(issueDoc.status());
      assertThat(issueForIngestion.isHotspot()).isTrue();
      assertThat(issueForIngestion.severity()).isEqualTo(Severity.valueOf(issueDoc.severity()).ordinal());
    }

    @Test
    void shouldIgnoreIssuesDocsWithIssuesForNonExistingRules() {
      IssueDoc issueDoc = getIssueDoc(RuleType.SECURITY_HOTSPOT);

      var dbSession = mock(DbSession.class);
      RuleDao ruleDao = getRuleDao();
      when(ruleDao.selectByUuids(dbSession, Set.of(RULE_UUID))).thenReturn(List.of());

      var issueWithRuleUuidDtos = getIssueWithRuleUuidDtos(issueDoc);
      var issuesForIngestion = underTest.transformToRepositoryRuleIssuesDtos(issueWithRuleUuidDtos, dbSession);

      assertThat(issuesForIngestion).isEmpty();
    }
  }

  private RuleDao getRuleDao() {
    RuleDao ruleDao = mock(RuleDao.class);
    when(dbClient.ruleDao()).thenReturn(ruleDao);
    return ruleDao;
  }

  private static List<IssueStatsIndexer.IssueWithRuleUuidDto> getIssueWithRuleUuidDtos(IssueDoc issueDoc) {
    return List.of(new IssueStatsIndexer.IssueWithRuleUuidDto(
      issueDoc.ruleUuid(),
      issueDoc.status(),
      RuleType.SECURITY_HOTSPOT.equals(issueDoc.type()),
      Severity.valueOf(issueDoc.severity()).ordinal()
    ));
  }

  private static RuleDto getRuleDto() {
    RuleDto ruleDto = new RuleDto();
    ruleDto.setUuid(RULE_UUID);
    ruleDto.setRepositoryKey("REPO_KEY");
    ruleDto.setRuleKey("RULE_KEY");
    return ruleDto;
  }

  private static IssueDoc getIssueDoc(RuleType ruleType) {
    IssueDoc issueDoc = new IssueDoc();
    issueDoc.setRuleUuid(RULE_UUID);
    issueDoc.setStatus("OPEN");
    issueDoc.setType(ruleType);
    issueDoc.setSeverity("MAJOR");
    return issueDoc;
  }
}
