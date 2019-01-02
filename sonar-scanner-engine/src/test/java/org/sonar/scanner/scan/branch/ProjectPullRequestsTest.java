/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.scan.branch;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectPullRequestsTest {

  private static AtomicInteger counter = new AtomicInteger();

  @Test
  public void should_pick_the_latest_branch_by_analysis_date() {
    String branchName = "dummyBranch";
    long date = 1000;

    PullRequestInfo pr1 = newPullRequestInfo(branchName, date - 2);
    PullRequestInfo pr2 = newPullRequestInfo(branchName, date - 1);
    PullRequestInfo latest = newPullRequestInfo(branchName, date);

    PullRequestInfo pullRequestInfo = getPullRequestInfo(branchName, pr1, pr2, latest);
    assertThat(pullRequestInfo.getKey()).isEqualTo(latest.getKey());
    assertThat(pullRequestInfo.getAnalysisDate()).isEqualTo(date);
  }

  @Test
  public void should_not_crash_when_cannot_pick_a_unique_branch() {
    String branchName = "dummyBranch";
    long date = 1000;

    PullRequestInfo pr1 = newPullRequestInfo(branchName, date);
    PullRequestInfo pr2 = newPullRequestInfo(branchName, date);

    PullRequestInfo pullRequestInfo = getPullRequestInfo(branchName, pr1, pr2);
    assertThat(pullRequestInfo.getAnalysisDate()).isEqualTo(date);
  }

  @Test
  public void should_get_correct_branch() {
    long date = 1000;

    PullRequestInfo foo = newPullRequestInfo("foo", date);
    PullRequestInfo bar = newPullRequestInfo("bar", date);

    assertThat(getPullRequestInfo("foo", foo, bar).getBranch()).isEqualTo("foo");
    assertThat(getPullRequestInfo("bar", foo, bar).getBranch()).isEqualTo("bar");
  }

  private PullRequestInfo newPullRequestInfo(String branchName, long date) {
    return new PullRequestInfo("pr" + counter.incrementAndGet(), branchName, null, date);
  }

  private PullRequestInfo getPullRequestInfo(String branchName, PullRequestInfo ...prs) {
    return new ProjectPullRequests(Arrays.asList(prs)).get(branchName);
  }

}
