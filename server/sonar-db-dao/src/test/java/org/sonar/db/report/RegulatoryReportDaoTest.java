/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.report;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class RegulatoryReportDaoTest {
  private static final String PROJECT_UUID = "prj_uuid";
  private static final String PROJECT_KEY = "prj_key";
  private static final String FILE_UUID = "file_uuid";
  private static final String FILE_KEY = "file_key";
  private static final String BRANCH_UUID = "branch_uuid";
  private static final String BRANCH_NAME = "branch";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final RegulatoryReportDao underTest = db.getDbClient().regulatoryReportDao();
  private ComponentDto project;
  private RuleDto rule;
  private RuleDto hotspotRule;
  private ComponentDto file;

  @Before
  public void prepare() {
    rule = db.rules().insertRule();
    hotspotRule = db.rules().insertHotspotRule();
    project = db.components().insertPrivateProject(t -> t.setBranchUuid(PROJECT_UUID).setUuid(PROJECT_UUID).setKey(PROJECT_KEY));
    file = db.components().insertComponent(newFileDto(project).setUuid(FILE_UUID).setKey(FILE_KEY));
  }

  @Test
  public void scrollIssues_returns_all_non_closed_issues_for_project() {
    IssueDto issue1 = db.issues().insertIssue(rule, project, file, i -> i.setType(RuleType.BUG).setStatus("OPEN").setResolution(null));
    IssueDto issue2 = db.issues().insertIssue(rule, project, file, i -> i.setType(RuleType.VULNERABILITY).setStatus("CONFIRMED").setResolution(null));
    IssueDto issue3 = db.issues().insertHotspot(hotspotRule, project, file, i -> i.setStatus("RESOLVED").setResolution(RESOLUTION_WONT_FIX));
    IssueDto issueCodeSmell = db.issues().insertIssue(rule, project, file, i -> i.setType(RuleType.CODE_SMELL).setStatus("RESOLVED").setResolution(RESOLUTION_WONT_FIX));

    // comments
    db.issues().insertChange(issue1, ic -> ic.setChangeData("c1").setIssueChangeCreationDate(1000L).setChangeType("comment"));
    db.issues().insertChange(issue1, ic -> ic.setChangeData("c2").setIssueChangeCreationDate(2000L).setChangeType("comment"));
    db.issues().insertChange(issue1, ic -> ic.setChangeData("c3").setIssueChangeCreationDate(3000L).setChangeType("diff").setKey(null));

    db.issues().insertChange(issue2, ic -> ic.setChangeData("c4").setIssueChangeCreationDate(4000L).setChangeType("diff").setKey(null));

    // not returned
    IssueDto issue4 = db.issues().insertIssue(rule, project, file, i -> i.setStatus("CLOSED").setResolution(null));
    ComponentDto otherProject = db.components().insertPrivateProject();
    ComponentDto otherFile = db.components().insertComponent(newFileDto(otherProject));
    IssueDto issue5 = db.issues().insertIssue(rule, otherProject, otherFile);

    List<IssueFindingDto> issues = new ArrayList<>();
    underTest.scrollIssues(db.getSession(), PROJECT_UUID, result -> issues.add(result.getResultObject()));
    assertThat(issues).extracting(IssueFindingDto::getKey).containsOnly(issue1.getKey(), issue2.getKey(), issue3.getKey());

    // check fields
    IssueFindingDto issue = issues.stream().filter(i -> i.getKey().equals(issue1.getKey())).findFirst().get();
    assertThat(issue.getFileName()).isEqualTo(file.path());
    assertThat(issue.getRuleName()).isEqualTo(rule.getName());
    assertThat(issue.getRuleKey()).isEqualTo(rule.getRuleKey());
    assertThat(issue.getRuleRepository()).isEqualTo(rule.getRepositoryKey());
    assertThat(issue.getMessage()).isEqualTo(issue1.getMessage());
    assertThat(issue.getLine()).isEqualTo(issue1.getLine());
    assertThat(issue.getSeverity()).isEqualTo(issue1.getSeverity());
    assertThat(issue.getType().getDbConstant()).isEqualTo(issue1.getType());
    assertThat(issue.getSecurityStandards()).isEqualTo(rule.getSecurityStandards());
    assertThat(issue.isManualSeverity()).isEqualTo(issue1.isManualSeverity());
    assertThat(issue.getCreationDate()).isEqualTo(issue1.getIssueCreationTime());
    assertThat(issue.isNewCodeReferenceIssue()).isEqualTo(issue1.isNewCodeReferenceIssue());
    assertThat(issue.getResolution()).isEqualTo(issue1.getResolution());
    assertThat(issue.getStatus()).isEqualTo(issue1.getStatus());
    assertThat(issue.getComments()).containsExactly("c1", "c2");
  }
}
