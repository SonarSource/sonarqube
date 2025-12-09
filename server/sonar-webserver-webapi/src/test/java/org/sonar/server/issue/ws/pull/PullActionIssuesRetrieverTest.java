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
package org.sonar.server.issue.ws.pull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.DbClient;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueQueryParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullActionIssuesRetrieverTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final String branchUuid = "master-branch-uuid";
  private final List<String> languages = List.of("java");
  private final List<String> ruleRepositories = List.of("js-security", "java");
  private final Long defaultChangedSince = 1_000_000L;

  private final IssueQueryParams queryParams = new IssueQueryParams(branchUuid, languages, ruleRepositories, null,
    false, defaultChangedSince);
  private final IssueDao issueDao = mock(IssueDao.class);

  @Before
  public void before() {
    when(dbClient.issueDao()).thenReturn(issueDao);
  }

  @Test
  public void processIssuesByBatch_givenNoIssuesReturnedByDatabase_noIssuesConsumed() {
    var pullActionIssuesRetriever = new PullActionIssuesRetriever(dbClient, queryParams);
    when(issueDao.selectByBranch(any(), any(), any()))
      .thenReturn(List.of());
    List<IssueDto> returnedDtos = new ArrayList<>();
    Consumer<List<IssueDto>> listConsumer = returnedDtos::addAll;

    pullActionIssuesRetriever.processIssuesByBatch(dbClient.openSession(true), Set.of(), listConsumer, issueDto -> true);

    assertThat(returnedDtos).isEmpty();
  }

  @Test
  public void processIssuesByBatch_givenThousandOneIssuesReturnedByDatabase_thousandOneIssuesConsumed() {
    var pullActionIssuesRetriever = new PullActionIssuesRetriever(dbClient, queryParams);
    List<IssueDto> thousandIssues = IntStream.rangeClosed(1, 1000).mapToObj(i -> new IssueDto().setKee(Integer.toString(i))).toList();
    IssueDto singleIssue = new IssueDto().setKee("kee");
    when(issueDao.selectByBranch(any(), any(), any()))
      .thenReturn(thousandIssues)
      .thenReturn(List.of(singleIssue));
    List<IssueDto> returnedDtos = new ArrayList<>();
    Consumer<List<IssueDto>> listConsumer = returnedDtos::addAll;

    Set<String> thousandIssueUuidsSnapshot = thousandIssues.stream().map(IssueDto::getKee).collect(Collectors.toSet());
    thousandIssueUuidsSnapshot.add(singleIssue.getKee());
    pullActionIssuesRetriever.processIssuesByBatch(dbClient.openSession(true), thousandIssueUuidsSnapshot, listConsumer, issueDto -> true);

    ArgumentCaptor<Set<String>> uuidsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(issueDao, times(2)).selectByBranch(any(), uuidsCaptor.capture(), any());
    List<Set<String>> capturedSets = uuidsCaptor.getAllValues();
    assertThat(capturedSets.get(0)).hasSize(1000);
    assertThat(capturedSets.get(1)).hasSize(1);

    assertThat(returnedDtos).hasSize(1001);
  }

  @Test
  public void processIssuesByBatch_correctly_processes_all_issues_regardless_of_creation_timestamp() {
    var pullActionIssuesRetriever = new PullActionIssuesRetriever(dbClient, queryParams);
    List<IssueDto> issuesWithSameCreationTimestamp = IntStream.rangeClosed(1, 100).mapToObj(i -> new IssueDto()
      .setKee(Integer.toString(i)).setCreatedAt(100L)).toList();
    when(issueDao.selectByBranch(any(), any(), any()))
      .thenReturn(issuesWithSameCreationTimestamp);
    List<IssueDto> returnedDtos = new ArrayList<>();
    Consumer<List<IssueDto>> listConsumer = returnedDtos::addAll;

    Set<String> issueKeysSnapshot = issuesWithSameCreationTimestamp.stream().map(IssueDto::getKee).collect(Collectors.toSet());
    pullActionIssuesRetriever.processIssuesByBatch(dbClient.openSession(true), issueKeysSnapshot, listConsumer, issueDto -> true);

    assertThat(returnedDtos).hasSize(100);
  }
}
