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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.TaintChecker.EXTRA_TAINT_REPOSITORIES;

public class TaintCheckerTest {
  private final Configuration configuration = mock(Configuration.class);
  private final TaintChecker underTest = new TaintChecker(configuration);

  @Test
  public void test_getTaintIssuesOnly() {
    List<IssueDto> taintIssues = underTest.getTaintIssuesOnly(getIssues());

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

    List<IssueDto> standardIssues = underTest.getStandardIssuesOnly(getIssues());

    assertThat(standardIssues).hasSize(3);
    assertThat(standardIssues.get(0).getKey()).isEqualTo("standardIssue1");
    assertThat(standardIssues.get(1).getKey()).isEqualTo("standardIssue2");
    assertThat(standardIssues.get(2).getKey()).isEqualTo("standardIssue3");
  }

  @Test
  public void test_mapIssuesByTaintStatus() {
    Map<Boolean, List<IssueDto>> issuesByTaintStatus = underTest.mapIssuesByTaintStatus(getIssues());

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

  @Test
  public void test_getTaintRepositories() {
    assertThat(underTest.getTaintRepositories())
      .hasSize(6)
      .containsExactlyInAnyOrder("roslyn.sonaranalyzer.security.cs", "javasecurity", "jssecurity",
        "tssecurity", "phpsecurity", "pythonsecurity");
  }

  @Test
  public void test_getTaintRepositories_withExtraReposFromConfiguration() {
    when(configuration.hasKey(EXTRA_TAINT_REPOSITORIES)).thenReturn(true);
    when(configuration.getStringArray(EXTRA_TAINT_REPOSITORIES)).thenReturn(new String[]{"extra-1", "extra-2"});
    TaintChecker underTest = new TaintChecker(configuration);
    assertThat(underTest.getTaintRepositories())
      .hasSize(8)
      .containsExactlyInAnyOrder("roslyn.sonaranalyzer.security.cs", "javasecurity", "jssecurity",
        "tssecurity", "phpsecurity", "pythonsecurity", "extra-1", "extra-2");
  }

  @Test
  public void test_isTaintVulnerability() {
    DefaultIssue taintWithoutLocation = createIssueWithRepository("noTaintIssue", "roslyn.sonaranalyzer.security.cs")
      .toDefaultIssue();
    DefaultIssue taint = createIssueWithRepository("taintIssue", "roslyn.sonaranalyzer.security.cs")
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder().build())
        .build())
      .toDefaultIssue();
    DefaultIssue issue = createIssueWithRepository("standardIssue", "java")
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder().build())
        .build())
      .toDefaultIssue();
    DefaultIssue hotspot = createIssueWithRepository("hotspot", "roslyn.sonaranalyzer.security.cs",
      RuleType.SECURITY_HOTSPOT).toDefaultIssue();

    assertThat(underTest.isTaintVulnerability(taintWithoutLocation)).isFalse();
    assertThat(underTest.isTaintVulnerability(taint)).isTrue();
    assertThat(underTest.isTaintVulnerability(issue)).isFalse();
    assertThat(underTest.isTaintVulnerability(hotspot)).isFalse();
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
    return createIssueWithRepository(issueKey, repository, null);
  }


  private IssueDto createIssueWithRepository(String issueKey, String repository, @Nullable RuleType ruleType) {
    IssueDto issueDto = new IssueDto();
    issueDto.setStatus("OPEN");
    issueDto.setKee(issueKey);
    issueDto.setRuleKey(repository, "S1");
    issueDto.setType(ruleType == null ? RuleType.VULNERABILITY : ruleType);
    return issueDto;
  }

}
