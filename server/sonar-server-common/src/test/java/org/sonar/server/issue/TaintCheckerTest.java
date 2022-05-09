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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sonar.db.issue.IssueDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.issue.TaintChecker.getStandardIssuesOnly;
import static org.sonar.server.issue.TaintChecker.getTaintIssuesOnly;
import static org.sonar.server.issue.TaintChecker.mapIssuesByTaintStatus;

public class TaintCheckerTest {

  @Test
  public void test_getTaintIssuesOnly() {

    List<IssueDto> taintIssues = getTaintIssuesOnly(getIssues());

    assertThat(taintIssues).hasSize(6);
    assertThat(taintIssues.get(0).getKey()).isEqualTo("taintIssue1");
    assertThat(taintIssues.get(1).getKey()).isEqualTo("taintIssue2");
    assertThat(taintIssues.get(2).getKey()).isEqualTo("taintIssue3");
    assertThat(taintIssues.get(3).getKey()).isEqualTo("taintIssue4");
    assertThat(taintIssues.get(4).getKey()).isEqualTo("taintIssue5");
    assertThat(taintIssues.get(5).getKey()).isEqualTo("taintIssue6");

  }

  @Test
  public void test_getStandardIssuesOnly() {

    List<IssueDto> standardIssues = getStandardIssuesOnly(getIssues());

    assertThat(standardIssues).hasSize(3);
    assertThat(standardIssues.get(0).getKey()).isEqualTo("standardIssue1");
    assertThat(standardIssues.get(1).getKey()).isEqualTo("standardIssue2");
    assertThat(standardIssues.get(2).getKey()).isEqualTo("standardIssue3");
  }

  @Test
  public void test_mapIssuesByTaintStatus() {
    Map<Boolean, List<IssueDto>> issuesByTaintStatus = mapIssuesByTaintStatus(getIssues());

    assertThat(issuesByTaintStatus.keySet()).hasSize(2);
    assertThat(issuesByTaintStatus.get(true)).hasSize(6);
    assertThat(issuesByTaintStatus.get(false)).hasSize(3);

    assertThat(issuesByTaintStatus.get(true).get(0).getKey()).isEqualTo("taintIssue1");
    assertThat(issuesByTaintStatus.get(true).get(1).getKey()).isEqualTo("taintIssue2");
    assertThat(issuesByTaintStatus.get(true).get(2).getKey()).isEqualTo("taintIssue3");
    assertThat(issuesByTaintStatus.get(true).get(3).getKey()).isEqualTo("taintIssue4");
    assertThat(issuesByTaintStatus.get(true).get(4).getKey()).isEqualTo("taintIssue5");
    assertThat(issuesByTaintStatus.get(true).get(5).getKey()).isEqualTo("taintIssue6");

    assertThat(issuesByTaintStatus.get(false).get(0).getKey()).isEqualTo("standardIssue1");
    assertThat(issuesByTaintStatus.get(false).get(1).getKey()).isEqualTo("standardIssue2");
    assertThat(issuesByTaintStatus.get(false).get(2).getKey()).isEqualTo("standardIssue3");
  }

  private List<IssueDto> getIssues() {
    List<IssueDto> issues = new ArrayList<>();

    issues.add(createIssueWithRepository("taintIssue1", "roslyn.sonaranalyzer.security.cs"));
    issues.add(createIssueWithRepository("taintIssue2", "javasecurity"));
    issues.add(createIssueWithRepository("taintIssue3", "jssecurity"));
    issues.add(createIssueWithRepository("taintIssue4", "tssecurity"));
    issues.add(createIssueWithRepository("taintIssue5", "phpsecurity"));
    issues.add(createIssueWithRepository("taintIssue6", "pythonsecurity"));

    issues.add(createIssueWithRepository("standardIssue1", "java"));
    issues.add(createIssueWithRepository("standardIssue2", "python"));
    issues.add(createIssueWithRepository("standardIssue3", "js"));

    return issues;
  }

  private IssueDto createIssueWithRepository(String issueKey, String repository) {
    IssueDto issueDto = new IssueDto();
    issueDto.setKee(issueKey);
    issueDto.setRuleKey(repository, "S1");
    return issueDto;
  }

}
