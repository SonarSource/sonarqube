/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.rule.ServerRuleFinder;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebIssueStorageTest {

  private static final String PROJECT_UUID = "project-uuid";
  private static final String BRANCH_UUID = "branch-uuid";
  private static final long NOW = 1_700_000_000_000L;

  private final System2 system2 = mock(System2.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final ServerRuleFinder ruleFinder = mock(ServerRuleFinder.class);
  private final IssueIndexer indexer = mock(IssueIndexer.class);
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final IssueUpdatedTelemetryPublisher issueUpdatedTelemetryPublisher = mock(IssueUpdatedTelemetryPublisher.class);

  private final IssueDao issueDao = mock(IssueDao.class);
  private final ComponentDao componentDao = mock(ComponentDao.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final DbSession updateSession = mock(DbSession.class);
  private final IssueChangeMapper issueChangeMapper = mock(IssueChangeMapper.class);

  private final WebIssueStorage underTest = new WebIssueStorage(system2, dbClient, ruleFinder, indexer, uuidFactory, issueUpdatedTelemetryPublisher);

  @BeforeEach
  void setUp() {
    when(system2.now()).thenReturn(NOW);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.openSession(false)).thenReturn(updateSession);
    when(dbSession.getMapper(IssueChangeMapper.class)).thenReturn(issueChangeMapper);
    when(updateSession.getMapper(IssueChangeMapper.class)).thenReturn(issueChangeMapper);
    when(ruleFinder.findDtoByKey(any())).thenReturn(Optional.of(new RuleDto().setUuid("rule-uuid")));
    when(componentDao.selectOrFailByUuid(any(), any())).thenReturn(new ComponentDto());
  }

  @Test
  void save_whenIssueIsUpdated_delegatesToIssueUpdatedTelemetryPublisherWithSameSessionAndIssue() {
    DefaultIssue issue = existingIssue("issue1").setNew(false);

    underTest.save(dbSession, singletonList(issue));

    ArgumentCaptor<Collection<DefaultIssue>> captor = capturePublishedIssues();
    assertThat(captor.getValue()).containsExactly(issue);
  }

  @Test
  void save_whenIssueIsNew_doesNotIncludeItInTheTelemetryPublisherCall() {
    DefaultIssue issue = existingIssue("issue1").setNew(true);

    underTest.save(dbSession, singletonList(issue));

    ArgumentCaptor<Collection<DefaultIssue>> captor = capturePublishedIssues();
    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  void save_whenMixOfNewAndUpdatedIssues_onlyPassesUpdatedIssuesToTelemetryPublisher() {
    DefaultIssue newIssue = existingIssue("new-issue").setNew(true);
    DefaultIssue updatedIssue = existingIssue("updated-issue").setNew(false);

    underTest.save(dbSession, List.of(newIssue, updatedIssue));

    ArgumentCaptor<Collection<DefaultIssue>> captor = capturePublishedIssues();
    assertThat(captor.getValue()).containsExactly(updatedIssue);
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<Collection<DefaultIssue>> capturePublishedIssues() {
    ArgumentCaptor<Collection<DefaultIssue>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(issueUpdatedTelemetryPublisher).publish(org.mockito.ArgumentMatchers.eq(dbSession), captor.capture());
    return captor;
  }

  private static DefaultIssue existingIssue(String key) {
    return new DefaultIssue()
      .setKey(key)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of("java", "S1234"))
      .setProjectUuid(PROJECT_UUID)
      .setComponentUuid("component-" + key)
      .setBranchUuid(BRANCH_UUID)
      .setCreationDate(new Date(NOW))
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null);
  }
}
