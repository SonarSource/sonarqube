/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueQueryParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PullActionIssuesRetrieverTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final String projectUuid = "default-project-uuid";
  private final String branchName = "master";
  private final List<String> languages = List.of("java");
  private final List<String> ruleRepositories = List.of("js-security", "java");
  private final Long defaultChangedSince = 1_000_000L;

  private final IssueQueryParams queryParams = new IssueQueryParams(projectUuid, branchName, languages, ruleRepositories, false,
    defaultChangedSince);
  private final IssueDao issueDao = mock(IssueDao.class);

  @Before
  public void before() {
    when(dbClient.issueDao()).thenReturn(issueDao);
  }

  @Test
  public void processIssuesByBatch_givenNoIssuesReturnedByDatabase_noIssuesConsumed() {
    var pullActionIssuesRetriever = new PullActionIssuesRetriever(dbClient, queryParams);
    when(issueDao.selectByBranch(any(), any(), anyInt()))
      .thenReturn(List.of());
    List<IssueDto> returnedDtos = new ArrayList<>();
    Consumer<List<IssueDto>> listConsumer = returnedDtos::addAll;

    pullActionIssuesRetriever.processIssuesByBatch(dbClient.openSession(true), listConsumer);

    assertThat(returnedDtos).isEmpty();
  }

  @Test
  public void processIssuesByBatch_givenThousandOneIssuesReturnedByDatabase_thousandOneIssuesConsumed() {
    var pullActionIssuesRetriever = new PullActionIssuesRetriever(dbClient, queryParams);
    List<IssueDto> thousandIssues = IntStream.rangeClosed(1, 1000).mapToObj(i -> new IssueDto()).collect(Collectors.toList());
    when(issueDao.selectByBranch(any(), any(), anyInt()))
      .thenReturn(thousandIssues)
      .thenReturn(List.of(new IssueDto()));
    List<IssueDto> returnedDtos = new ArrayList<>();
    Consumer<List<IssueDto>> listConsumer = returnedDtos::addAll;

    pullActionIssuesRetriever.processIssuesByBatch(dbClient.openSession(true), listConsumer);

    assertThat(returnedDtos).hasSize(1001);
  }
}
