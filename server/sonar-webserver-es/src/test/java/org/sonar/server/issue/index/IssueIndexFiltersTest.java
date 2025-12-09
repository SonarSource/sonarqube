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
package org.sonar.server.issue.index;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.issue.LinkedTicketStatus;
import org.sonar.core.rule.RuleType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueDocTesting;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.view.index.ViewDoc;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.rules.CleanCodeAttributeCategory.ADAPTABLE;
import static org.sonar.api.rules.CleanCodeAttributeCategory.CONSISTENT;
import static org.sonar.api.rules.CleanCodeAttributeCategory.INTENTIONAL;
import static org.sonar.api.rules.CleanCodeAttributeCategory.RESPONSIBLE;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.issue.IssueDocTesting.newDoc;
import static org.sonar.server.issue.IssueDocTesting.newDocForProject;

class IssueIndexFiltersTest extends IssueIndexTestCommon {

  @Test
  void filter_by_keys() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDoc("I1", project.uuid(), newFileDto(project)),
      newDoc("I2", project.uuid(), newFileDto(project)));

    assertThatSearchReturnsOnly(IssueQuery.builder().issueKeys(asList("I1", "I2")), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().issueKeys(singletonList("I1")), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().issueKeys(asList("I3", "I4")));
  }

  @Test
  void filter_by_compliance_standard() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(IssueDocTesting.
      newDoc("I1", project.uuid(), true, newFileDto(project)).setRuleUuid("r1"),
      newDoc("I2", project.uuid(), true, newFileDto(project)).setRuleUuid("r1"),
      newDoc("I3", project.uuid(), true, newFileDto(project)).setRuleUuid("r2")
    );

    assertThatSearchReturnsOnly(IssueQuery.builder().complianceCategoryRules(of("r1", "r2")), "I1", "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().complianceCategoryRules(of("r1")), "I1", "I2");
  }

  @Test
  void filter_by_projects() {
    ComponentDto project = newPrivateProjectDto();

    indexIssues(
      newDocForProject("I1", project),
      newDoc("I2", project.uuid(), newFileDto(project)));

    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())), "I1", "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList("unknown")));
  }

  @Test
  void filter_by_components_on_contextualized_search() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file1 = newFileDto(project);
    String view = "ABCD";
    indexView(view, singletonList(project.uuid()));

    indexIssues(
      newDocForProject("I1", project),
      newDoc("I2", project.uuid(), file1));

    assertThatSearchReturnsOnly(IssueQuery.builder().files(asList(file1.path())), "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().files(singletonList(file1.path())), "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view)), "I1", "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList("unknown")));
  }

  @Test
  void filter_by_components_on_non_contextualized_search() {
    ComponentDto project = newPrivateProjectDto("project");
    ComponentDto file1 = newFileDto(project, null, "file1");
    String view = "ABCD";
    indexView(view, singletonList(project.uuid()));

    indexIssues(
      newDocForProject("I1", project),
      newDoc("I2", project.uuid(), file1));

    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList("unknown")));
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().files(singletonList(file1.path())), "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().files(asList(file1.path())), "I2");
  }

  @Test
  void filter_by_directories() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file1 = newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = newFileDto(project).setPath("F2.xoo");

    indexIssues(
      newDoc("I1", project.uuid(), file1).setDirectoryPath("/src/main/xoo"),
      newDoc("I2", project.uuid(), file2).setDirectoryPath("/"));

    assertThatSearchReturnsOnly(IssueQuery.builder().directories(singletonList("/src/main/xoo")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().directories(singletonList("/")), "I1", "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().directories(singletonList("unknown")));
  }

  @Test
  void filter_by_portfolios() {
    ComponentDto portfolio1 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto portfolio2 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project1));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    IssueDoc issueOnProject1 = newDocForProject(project1);
    IssueDoc issueOnFile = newDoc(file, project1.uuid());
    IssueDoc issueOnProject2 = newDocForProject(project2);

    indexIssues(issueOnProject1, issueOnFile, issueOnProject2);
    indexView(portfolio1.uuid(), singletonList(project1.uuid()));
    indexView(portfolio2.uuid(), singletonList(project2.uuid()));

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(portfolio1.uuid())), issueOnProject1.key(), issueOnFile.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(portfolio2.uuid())), issueOnProject2.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(asList(portfolio1.uuid(), portfolio2.uuid())), issueOnProject1.key(), issueOnFile.key(), issueOnProject2.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(portfolio1.uuid())).projectUuids(singletonList(project1.uuid())), issueOnProject1.key(),
      issueOnFile.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(portfolio1.uuid())).files(singletonList(file.path())), issueOnFile.key());
    assertThatSearchReturnsEmpty(IssueQuery.builder().viewUuids(singletonList("unknown")));
  }

  @Test
  void filter_by_portfolios_not_having_projects() {
    ComponentDto project1 = newPrivateProjectDto();
    ComponentDto file1 = newFileDto(project1);
    indexIssues(newDoc("I2", project1.uuid(), file1));
    String view1 = "ABCD";
    indexView(view1, emptyList());

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view1)));
  }

  @Test
  void do_not_return_issues_from_project_branch_when_filtering_by_portfolios() {
    ComponentDto portfolio = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto projectBranch = db.components().insertProjectBranch(project);
    ComponentDto fileOnProjectBranch = db.components().insertComponent(newFileDto(projectBranch));
    indexView(portfolio.uuid(), singletonList(project.uuid()));

    IssueDoc issueOnProject = newDocForProject(project);
    IssueDoc issueOnProjectBranch = newDoc(projectBranch, project.uuid());
    IssueDoc issueOnFileOnProjectBranch = newDoc(fileOnProjectBranch, projectBranch.uuid());
    indexIssues(issueOnProject, issueOnFileOnProjectBranch, issueOnProjectBranch);

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(portfolio.uuid())), issueOnProject.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(portfolio.uuid())).projectUuids(singletonList(project.uuid())),
      issueOnProject.key());
    assertThatSearchReturnsEmpty(IssueQuery.builder().viewUuids(singletonList(portfolio.uuid())).projectUuids(singletonList(projectBranch.uuid())));
  }

  @Test
  void filter_one_issue_by_project_and_branch() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto anotherbBranch = db.components().insertProjectBranch(project);

    IssueDoc issueOnProject = newDocForProject(project);
    IssueDoc issueOnBranch = newDoc(branch, project.uuid());
    IssueDoc issueOnAnotherBranch = newDoc(anotherbBranch, project.uuid());
    indexIssues(issueOnProject, issueOnBranch, issueOnAnotherBranch);

    assertThatSearchReturnsOnly(IssueQuery.builder().branchUuid(branch.uuid()).mainBranch(false), issueOnBranch.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().componentUuids(singletonList(branch.uuid())).branchUuid(branch.uuid()).mainBranch(false), issueOnBranch.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())).branchUuid(branch.uuid()).mainBranch(false), issueOnBranch.key());
    assertThatSearchReturnsOnly(
      IssueQuery.builder().componentUuids(singletonList(branch.uuid())).projectUuids(singletonList(project.uuid())).branchUuid(branch.uuid()).mainBranch(false),
      issueOnBranch.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())).mainBranch(null), issueOnProject.key(), issueOnBranch.key(),
      issueOnAnotherBranch.key());
    assertThatSearchReturnsEmpty(IssueQuery.builder().branchUuid("unknown"));
  }

  @Test
  void issues_from_branch_component_children() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch, project.uuid()));

    indexIssues(
      newDocForProject("I1", project),
      newDoc("I2", project.uuid(), projectFile),
      newDoc("I3", project.uuid(), branch),
      newDoc("I4", project.uuid(), branchFile));

    assertThatSearchReturnsOnly(IssueQuery.builder().branchUuid(branch.uuid()).mainBranch(false), "I3", "I4");
    assertThatSearchReturnsOnly(IssueQuery.builder().files(singletonList(branchFile.path())).branchUuid(branch.uuid()).mainBranch(false), "I4");
    assertThatSearchReturnsEmpty(IssueQuery.builder().files(singletonList(branchFile.uuid())).mainBranch(false).branchUuid("unknown"));
  }

  @Test
  void issues_from_main_branch() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);

    IssueDoc issueOnProject = newDocForProject(project);
    IssueDoc issueOnBranch = newDoc(branch, project.uuid());
    indexIssues(issueOnProject, issueOnBranch);

    assertThatSearchReturnsOnly(IssueQuery.builder().branchUuid(project.uuid()).mainBranch(true), issueOnProject.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().componentUuids(singletonList(project.uuid())).branchUuid(project.uuid()).mainBranch(true), issueOnProject.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())).branchUuid(project.uuid()).mainBranch(true), issueOnProject.key());
    assertThatSearchReturnsOnly(
      IssueQuery.builder().componentUuids(singletonList(project.uuid())).projectUuids(singletonList(project.uuid())).branchUuid(project.uuid()).mainBranch(true),
      issueOnProject.key());
  }

  @Test
  void branch_issues_are_ignored_when_no_branch_param() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    IssueDoc projectIssue = newDocForProject(project);
    IssueDoc branchIssue = newDoc(branch, project.uuid());
    indexIssues(projectIssue, branchIssue);

    assertThatSearchReturnsOnly(IssueQuery.builder(), projectIssue.key());
  }

  @Test
  void filter_by_main_application() {
    ComponentDto application1 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto application2 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project1));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    indexView(application1.uuid(), singletonList(project1.uuid()));
    indexView(application2.uuid(), singletonList(project2.uuid()));

    IssueDoc issueOnProject1 = newDocForProject(project1);
    IssueDoc issueOnFile = newDoc(file, project1.uuid());
    IssueDoc issueOnProject2 = newDocForProject(project2);
    indexIssues(issueOnProject1, issueOnFile, issueOnProject2);

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(application1.uuid())), issueOnProject1.key(), issueOnFile.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(application2.uuid())), issueOnProject2.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(asList(application1.uuid(), application2.uuid())), issueOnProject1.key(), issueOnFile.key(), issueOnProject2.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(application1.uuid())).projectUuids(singletonList(project1.uuid())), issueOnProject1.key(),
      issueOnFile.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(application1.uuid())).files(singletonList(file.path())), issueOnFile.key());
    assertThatSearchReturnsEmpty(IssueQuery.builder().viewUuids(singletonList("unknown")));
  }

  @Test
  void filter_by_application_branch() {
    ComponentDto application = db.components().insertPublicProject(c -> c.setQualifier(APP)).getMainBranchComponent();
    ComponentDto branch1 = db.components().insertProjectBranch(application);
    ComponentDto branch2 = db.components().insertProjectBranch(application);
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project1));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    indexView(branch1.uuid(), singletonList(project1.uuid()));
    indexView(branch2.uuid(), singletonList(project2.uuid()));

    IssueDoc issueOnProject1 = newDocForProject(project1);
    IssueDoc issueOnFile = newDoc(file, project1.uuid());
    IssueDoc issueOnProject2 = newDocForProject(project2);
    indexIssues(issueOnProject1, issueOnFile, issueOnProject2);

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(branch1.uuid())).branchUuid(branch1.uuid()).mainBranch(false),
      issueOnProject1.key(), issueOnFile.key());
    assertThatSearchReturnsOnly(
      IssueQuery.builder().viewUuids(singletonList(branch1.uuid())).projectUuids(singletonList(project1.uuid())).branchUuid(branch1.uuid()).mainBranch(false),
      issueOnProject1.key(), issueOnFile.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(branch1.uuid())).files(singletonList(file.path())).branchUuid(branch1.uuid()).mainBranch(false),
      issueOnFile.key());
    assertThatSearchReturnsEmpty(IssueQuery.builder().branchUuid("unknown"));
  }

  @Test
  void filter_by_application_branch_having_project_branches() {
    ComponentDto application = db.components().insertPublicProject(c -> c.setQualifier(APP).setKey("app")).getMainBranchComponent();
    ComponentDto applicationBranch1 = db.components().insertProjectBranch(application, a -> a.setKey("app-branch1"));
    ComponentDto applicationBranch2 = db.components().insertProjectBranch(application, a -> a.setKey("app-branch2"));
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setKey("prj1")).getMainBranchComponent();
    ComponentDto project1Branch1 = db.components().insertProjectBranch(project1);
    ComponentDto fileOnProject1Branch1 = db.components().insertComponent(newFileDto(project1Branch1));
    ComponentDto project1Branch2 = db.components().insertProjectBranch(project1);
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setKey("prj2")).getMainBranchComponent();
    indexView(applicationBranch1.uuid(), asList(project1Branch1.uuid(), project2.uuid()));
    indexView(applicationBranch2.uuid(), singletonList(project1Branch2.uuid()));

    IssueDoc issueOnProject1 = newDocForProject(project1);
    IssueDoc issueOnProject1Branch1 = newDoc(project1Branch1, project1.uuid());
    IssueDoc issueOnFileOnProject1Branch1 = newDoc(fileOnProject1Branch1, project1.uuid());
    IssueDoc issueOnProject1Branch2 = newDoc(project1Branch2, project1.uuid());
    IssueDoc issueOnProject2 = newDocForProject(project2);
    indexIssues(issueOnProject1, issueOnProject1Branch1, issueOnFileOnProject1Branch1, issueOnProject1Branch2, issueOnProject2);

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(applicationBranch1.uuid())).branchUuid(applicationBranch1.uuid()).mainBranch(false),
      issueOnProject1Branch1.key(), issueOnFileOnProject1Branch1.key(), issueOnProject2.key());
    assertThatSearchReturnsOnly(
      IssueQuery.builder().viewUuids(singletonList(applicationBranch1.uuid())).projectUuids(singletonList(project1.uuid())).branchUuid(applicationBranch1.uuid()).mainBranch(false),
      issueOnProject1Branch1.key(), issueOnFileOnProject1Branch1.key());
    assertThatSearchReturnsOnly(
      IssueQuery.builder().viewUuids(singletonList(applicationBranch1.uuid())).files(singletonList(fileOnProject1Branch1.path())).branchUuid(applicationBranch1.uuid())
        .mainBranch(false),
      issueOnFileOnProject1Branch1.key());
    assertThatSearchReturnsEmpty(
      IssueQuery.builder().viewUuids(singletonList(applicationBranch1.uuid())).projectUuids(singletonList("unknown")).branchUuid(applicationBranch1.uuid()).mainBranch(false));
  }

  @Test
  void filter_by_created_after_by_projects() {
    Date now = new Date();
    ComponentDto project1 = newPrivateProjectDto();
    IssueDoc project1Issue1 = newDocForProject(project1).setFuncCreationDate(addDays(now, -10));
    IssueDoc project1Issue2 = newDocForProject(project1).setFuncCreationDate(addDays(now, -20));
    ComponentDto project2 = newPrivateProjectDto();
    IssueDoc project2Issue1 = newDocForProject(project2).setFuncCreationDate(addDays(now, -15));
    IssueDoc project2Issue2 = newDocForProject(project2).setFuncCreationDate(addDays(now, -30));
    indexIssues(project1Issue1, project1Issue2, project2Issue1, project2Issue2);

    // Search for issues of project 1 having less than 15 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(Map.of(project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -15), true))),
      project1Issue1.key());

    // Search for issues of project 1 having less than 14 days and project 2 having less then 25 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(Map.of(
        project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -14), true),
        project2.uuid(), new IssueQuery.PeriodStart(addDays(now, -25), true))),
      project1Issue1.key(), project2Issue1.key());

    // Search for issues of project 1 having less than 30 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(Map.of(
        project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -30), true))),
      project1Issue1.key(), project1Issue2.key());

    // Search for issues of project 1 and project 2 having less than 5 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(Map.of(
        project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -5), true),
        project2.uuid(), new IssueQuery.PeriodStart(addDays(now, -5), true))));
  }

  @Test
  void filter_by_created_after_by_project_branches() {
    Date now = new Date();

    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDoc project1Issue1 = newDocForProject(project1).setFuncCreationDate(addDays(now, -10));
    IssueDoc project1Issue2 = newDocForProject(project1).setFuncCreationDate(addDays(now, -20));

    ComponentDto project1Branch1 = db.components().insertProjectBranch(project1);
    IssueDoc project1Branch1Issue1 = newDoc(project1Branch1, project1.uuid()).setFuncCreationDate(addDays(now, -10));
    IssueDoc project1Branch1Issue2 = newDoc(project1Branch1, project1.uuid()).setFuncCreationDate(addDays(now, -20));

    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    IssueDoc project2Issue1 = newDocForProject(project2).setFuncCreationDate(addDays(now, -15));
    IssueDoc project2Issue2 = newDocForProject(project2).setFuncCreationDate(addDays(now, -30));

    ComponentDto project2Branch1 = db.components().insertProjectBranch(project2);
    IssueDoc project2Branch1Issue1 = newDoc(project2Branch1, project2.uuid()).setFuncCreationDate(addDays(now, -15));
    IssueDoc project2Branch1Issue2 = newDoc(project2Branch1, project2.uuid()).setFuncCreationDate(addDays(now, -30));

    indexIssues(project1Issue1, project1Issue2, project2Issue1, project2Issue2,
      project1Branch1Issue1, project1Branch1Issue2, project2Branch1Issue1, project2Branch1Issue2);

    // Search for issues of project 1 branch 1 having less than 15 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .mainBranch(false)
      .createdAfterByProjectUuids(Map.of(project1Branch1.uuid(), new IssueQuery.PeriodStart(addDays(now, -15), true))),
      project1Branch1Issue1.key());

    // Search for issues of project 1 branch 1 having less than 14 days and project 2 branch 1 having less then 25 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .mainBranch(false)
      .createdAfterByProjectUuids(Map.of(
        project1Branch1.uuid(), new IssueQuery.PeriodStart(addDays(now, -14), true),
        project2Branch1.uuid(), new IssueQuery.PeriodStart(addDays(now, -25), true))),
      project1Branch1Issue1.key(), project2Branch1Issue1.key());

    // Search for issues of project 1 branch 1 having less than 30 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .mainBranch(false)
      .createdAfterByProjectUuids(Map.of(
        project1Branch1.uuid(), new IssueQuery.PeriodStart(addDays(now, -30), true))),
      project1Branch1Issue1.key(), project1Branch1Issue2.key());

    // Search for issues of project 1 branch 1 and project 2 branch 2 having less than 5 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .mainBranch(false)
      .createdAfterByProjectUuids(Map.of(
        project1Branch1.uuid(), new IssueQuery.PeriodStart(addDays(now, -5), true),
        project2Branch1.uuid(), new IssueQuery.PeriodStart(addDays(now, -5), true))));
  }

  @Test
  void filter_by_new_code_reference_by_projects() {
    ComponentDto project1 = newPrivateProjectDto();
    IssueDoc project1Issue1 = newDocForProject(project1).setIsNewCodeReference(true);
    IssueDoc project1Issue2 = newDocForProject(project1).setIsNewCodeReference(false);
    ComponentDto project2 = newPrivateProjectDto();
    IssueDoc project2Issue1 = newDocForProject(project2).setIsNewCodeReference(false);
    IssueDoc project2Issue2 = newDocForProject(project2).setIsNewCodeReference(true);
    indexIssues(project1Issue1, project1Issue2, project2Issue1, project2Issue2);

    // Search for issues of project 1 and project 2 that are new code on a branch using reference for new code
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .newCodeOnReferenceByProjectUuids(Set.of(project1.uuid(), project2.uuid())),
      project1Issue1.key(), project2Issue2.key());
  }

  @Test
  void filter_by_new_reference_branches() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDoc project1Issue1 = newDocForProject(project1).setIsNewCodeReference(true);
    IssueDoc project1Issue2 = newDocForProject(project1).setIsNewCodeReference(false);

    ComponentDto project1Branch1 = db.components().insertProjectBranch(project1);
    IssueDoc project1Branch1Issue1 = newDoc(project1Branch1, project1.uuid()).setIsNewCodeReference(false);
    IssueDoc project1Branch1Issue2 = newDoc(project1Branch1, project1.uuid()).setIsNewCodeReference(true);

    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    IssueDoc project2Issue1 = newDocForProject(project2).setIsNewCodeReference(true);
    IssueDoc project2Issue2 = newDocForProject(project2).setIsNewCodeReference(false);

    ComponentDto project2Branch1 = db.components().insertProjectBranch(project2);
    IssueDoc project2Branch1Issue1 = newDoc(project2Branch1, project2.uuid()).setIsNewCodeReference(false);
    IssueDoc project2Branch1Issue2 = newDoc(project2Branch1, project2.uuid()).setIsNewCodeReference(true);

    ComponentDto project3 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project3Branch1 = db.components().insertProjectBranch(project2);
    IssueDoc project3Issue1 = newDoc(project3Branch1, project3.uuid()).setFuncCreationDate(new Date(1000L));
    IssueDoc project3Issue2 = newDoc(project3Branch1, project3.uuid()).setFuncCreationDate(new Date(2000L));

    indexIssues(project1Issue1, project1Issue2, project2Issue1, project2Issue2,
      project1Branch1Issue1, project1Branch1Issue2, project2Branch1Issue1, project2Branch1Issue2, project3Issue1, project3Issue2);

    // Search for issues of project 1 branch 1 and project 2 branch 1 that are new code on a branch using reference for new code
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .mainBranch(false)
      .newCodeOnReferenceByProjectUuids(Set.of(project1Branch1.uuid(), project2Branch1.uuid()))
      .createdAfterByProjectUuids(Map.of(project3Branch1.uuid(), new IssueQuery.PeriodStart(new Date(1500), false))),
        project1Branch1Issue2.key(), project2Branch1Issue2.key(), project3Issue2.key());
  }

  @Test
  void filter_by_severities() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setSeverity(Severity.INFO),
      newDoc("I2", project.uuid(), file).setSeverity(Severity.MAJOR));

    assertThatSearchReturnsOnly(IssueQuery.builder().severities(asList(Severity.INFO, Severity.MAJOR)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().severities(singletonList(Severity.INFO)), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().severities(singletonList(Severity.BLOCKER)));
  }

  @Test
  void filter_by_statuses() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setStatus(Issue.STATUS_CLOSED),
      newDoc("I2", project.uuid(), file).setStatus(Issue.STATUS_OPEN));

    assertThatSearchReturnsOnly(IssueQuery.builder().statuses(asList(Issue.STATUS_CLOSED, Issue.STATUS_OPEN)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().statuses(singletonList(Issue.STATUS_CLOSED)), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().statuses(singletonList(Issue.STATUS_CONFIRMED)));
  }

  @Test
  void filter_by_resolutions() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      newDoc("I2", project.uuid(), file).setResolution(Issue.RESOLUTION_FIXED));

    assertThatSearchReturnsOnly(IssueQuery.builder().resolutions(asList(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().resolutions(singletonList(Issue.RESOLUTION_FALSE_POSITIVE)), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().resolutions(singletonList(Issue.RESOLUTION_REMOVED)));
  }

  @Test
  void filter_by_resolved() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED),
      newDoc("I2", project.uuid(), file).setStatus(Issue.STATUS_OPEN).setResolution(null),
      newDoc("I3", project.uuid(), file).setStatus(Issue.STATUS_OPEN).setResolution(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().resolved(true), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().resolved(false), "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().resolved(null), "I1", "I2", "I3");
  }

  @Test
  void filter_by_rules() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    RuleDto ruleDefinitionDto = newRule();
    db.rules().insert(ruleDefinitionDto);

    indexIssues(newDoc("I1", project.uuid(), file).setRuleUuid(ruleDefinitionDto.getUuid()));

    assertThatSearchReturnsOnly(IssueQuery.builder().ruleUuids(singletonList(ruleDefinitionDto.getUuid())), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().ruleUuids(singletonList("uuid-abc")));
  }

  @Test
  void filter_by_languages() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);
    RuleDto ruleDefinitionDto = newRule();
    db.rules().insert(ruleDefinitionDto);

    indexIssues(newDoc("I1", project.uuid(), file).setRuleUuid(ruleDefinitionDto.getUuid()).setLanguage("xoo"));

    assertThatSearchReturnsOnly(IssueQuery.builder().languages(singletonList("xoo")), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().languages(singletonList("unknown")));
  }

  @Test
  void filter_by_assignees() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setAssigneeUuid("steph-uuid"),
      newDoc("I2", project.uuid(), file).setAssigneeUuid("marcel-uuid"),
      newDoc("I3", project.uuid(), file).setAssigneeUuid(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().assigneeUuids(singletonList("steph-uuid")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().assigneeUuids(asList("steph-uuid", "marcel-uuid")), "I1", "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().assigneeUuids(singletonList("unknown")));
  }

  @Test
  void filter_by_assigned() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setAssigneeUuid("steph-uuid"),
      newDoc("I2", project.uuid(), file).setAssigneeUuid(null),
      newDoc("I3", project.uuid(), file).setAssigneeUuid(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().assigned(true), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().assigned(false), "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().assigned(null), "I1", "I2", "I3");
  }

  @Test
  void filter_by_authors() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setAuthorLogin("steph"),
      newDoc("I2", project.uuid(), file).setAuthorLogin("marcel"),
      newDoc("I3", project.uuid(), file).setAssigneeUuid(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().authors(singletonList("steph")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().authors(asList("steph", "marcel")), "I1", "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().authors(singletonList("unknown")));
  }

  @Test
  void filter_by_prioritized_rule() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setPrioritizedRule(true),
      newDoc("I2", project.uuid(), file).setPrioritizedRule(true),
      newDoc("I3", project.uuid(), file).setPrioritizedRule(false));

    assertThatSearchReturnsOnly(IssueQuery.builder().prioritizedRule(null), "I1", "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().prioritizedRule(true), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().prioritizedRule(false), "I3");
  }

  @Test
  void filter_by_linkedTicketStatus() {
    var project = newPrivateProjectDto();
    var file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setLinkedTicketStatus(LinkedTicketStatus.LINKED),
      newDoc("I2", project.uuid(), file).setLinkedTicketStatus(LinkedTicketStatus.NOT_LINKED),
      newDoc("I3", project.uuid(), file).setLinkedTicketStatus(LinkedTicketStatus.LINKED)
    );

    assertThatSearchReturnsOnly(IssueQuery.builder().linkedTicketStatuses(null), "I1", "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().linkedTicketStatuses(of(LinkedTicketStatus.LINKED)), "I1", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().linkedTicketStatuses(of(LinkedTicketStatus.NOT_LINKED)), "I2");
  }

  @Test
  void filter_by_issues_from_analyzer_update() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFromSonarQubeUpdate(true),
      newDoc("I2", project.uuid(), file).setFromSonarQubeUpdate(true),
      newDoc("I3", project.uuid(), file).setFromSonarQubeUpdate(false));

    assertThatSearchReturnsOnly(IssueQuery.builder().fromSonarQubeUpdate(null), "I1", "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().fromSonarQubeUpdate(true), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().fromSonarQubeUpdate(false), "I3");
  }

  @Test
  void filter_by_created_after() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", project.uuid(), file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-19")), "I1", "I2");
    // Lower bound is included
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-20")), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-21")), "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAfter(parseDate("2014-09-25")));
  }

  @Test
  void filter_by_created_before() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", project.uuid(), file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThatSearchReturnsEmpty(IssueQuery.builder().createdBefore(parseDate("2014-09-19")));
    // Upper bound is excluded
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdBefore(parseDate("2014-09-20")));
    assertThatSearchReturnsOnly(IssueQuery.builder().createdBefore(parseDate("2014-09-21")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().createdBefore(parseDate("2014-09-25")), "I1", "I2");
  }

  @Test
  void filter_by_created_after_and_before() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", project.uuid(), file).setFuncCreationDate(parseDate("2014-09-23")));

    // 19 < createdAt < 25
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-25")),
      "I1", "I2");

    // 20 < createdAt < 25: excludes first issue
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-25")), "I1", "I2");

    // 21 < createdAt < 25
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-25")), "I2");

    // 21 < createdAt < 24
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-24")), "I2");

    // 21 < createdAt < 23: excludes second issue
    assertThatSearchReturnsEmpty(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-23")));

    // 19 < createdAt < 21: only first issue
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-21")), "I1");

    // 20 < createdAt < 20: exception
    assertThatThrownBy(() -> underTest.search(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-20"))
      .build(), new SearchOptions()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void filter_by_created_after_and_before_take_into_account_timezone() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-20T00:00:00+0100")),
      newDoc("I2", project.uuid(), file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDateTime("2014-09-19T23:00:00+0000")).createdBefore(parseDateTime("2014-09-22T23:00:01+0000")),
      "I1", "I2");

    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAfter(parseDateTime("2014-09-19T23:00:01+0000")).createdBefore(parseDateTime("2014-09-22T23:00:00+0000")));
  }

  @Test
  void filter_by_created_before_must_be_lower_than_after() {
    try {
      underTest.search(IssueQuery.builder().createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-19")).build(),
        new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be larger or equal to end bound");
    }
  }

  @Test
  void fail_if_created_before_equals_created_after() {
    assertThatThrownBy(() -> underTest.search(IssueQuery.builder().createdAfter(parseDate("2014-09-20"))
      .createdBefore(parseDate("2014-09-20")).build(), new SearchOptions()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Start bound cannot be larger or equal to end bound");
  }

  @Test
  void filter_by_created_after_must_not_be_in_future() {
    try {
      underTest.search(IssueQuery.builder().createdAfter(new Date(Long.MAX_VALUE)).build(), new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be in the future");
    }
  }

  @Test
  void filter_by_created_at() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(newDoc("I1", project.uuid(), file).setFuncCreationDate(parseDate("2014-09-20")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAt(parseDate("2014-09-20")), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAt(parseDate("2014-09-21")));
  }

  @Test
  void filter_by_new_code_reference() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(newDoc("I1", project.uuid(), file).setIsNewCodeReference(true),
      newDoc("I2", project.uuid(), file).setIsNewCodeReference(false));

    assertThatSearchReturnsOnly(IssueQuery.builder().newCodeOnReference(true), "I1");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filter_by_cwe(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setCwe(asList("20", "564",
        "89", "943")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setCwe(singletonList("943")),
      newDoc("I3", project.uuid(), file));

    assertThatSearchReturnsOnly(IssueQuery.builder().cwe(singletonList("20")), "I1");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filter_by_owaspAsvs40_category(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(asList("1.1.1"
        , "1.2.2", "2.2.2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(asList("1.1.1"
        , "1.2.2")),
      newDoc("I3", project.uuid(), file));

    assertThatSearchReturnsOnly(IssueQuery.builder().owaspAsvs40(singletonList("1")), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().owaspAsvs40(singletonList("2")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().owaspAsvs40(singletonList("3")));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filter_by_owaspAsvs40_specific_requirement(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(asList("1.1.1"
        , "1.2.2", "2.2.2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(asList("1.1.1"
        , "1.2.2")),
      newDoc("I3", project.uuid(), file));

    assertThatSearchReturnsOnly(IssueQuery.builder().owaspAsvs40(singletonList("1.1.1")), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().owaspAsvs40(singletonList("2.2.2")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().owaspAsvs40(singletonList("3.3.3")));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filter_by_owaspAsvs40_level(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(asList("2.1.1"
        , "1.1.1", "1.11.3")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, BLOCKER)).setOwaspAsvs40(asList("1.1" +
        ".1", "1.11.3")),
      newDoc("I3", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, LOW)).setOwaspAsvs40(singletonList(
        "1.11.3")),
      newDoc("IError1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspAsvs40(asList("5" +
        ".5.1", "7.2.2", "10.2.6")),
      newDoc("IError2", project.uuid(), file));

    assertThatSearchReturnsOnly(
      IssueQuery.builder().owaspAsvs40(singletonList("1.1.1")).owaspAsvsLevel(1));
    assertThatSearchReturnsOnly(
      IssueQuery.builder().owaspAsvs40(singletonList("1.1.1")).owaspAsvsLevel(2),
      "I1", "I2");
    assertThatSearchReturnsOnly(
      IssueQuery.builder().owaspAsvs40(singletonList("1.1.1")).owaspAsvsLevel(3),
      "I1", "I2");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filter_by_owaspTop10(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setOwaspTop10(asList("a1",
        "a2")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setCwe(singletonList("a3")),
      newDoc("I3", project.uuid(), file));

    assertThatSearchReturnsOnly(IssueQuery.builder().owaspTop10(singletonList("a1")), "I1");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filter_by_sansTop25(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSansTop25(asList("porous" +
        "-defenses", "risky-resource", "insecure-interaction")),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSansTop25(singletonList(
        "porous-defenses")),
      newDoc("I3", project.uuid(), file));

    assertThatSearchReturnsOnly(IssueQuery.builder().sansTop25(singletonList("risky-resource")), "I1");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filter_by_sonarSecurity(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSonarSourceSecurityCategory(SQCategory.BUFFER_OVERFLOW),
      newDoc("I2", project.uuid(), file).setType(RuleType.VULNERABILITY).setImpacts(Map.of(SECURITY, HIGH)).setSonarSourceSecurityCategory(SQCategory.DOS),
      newDoc("I3", project.uuid(), file));

    assertThatSearchReturnsOnly(IssueQuery.builder().sonarsourceSecurity(singletonList("buffer-overflow")), "I1");
  }

  @Test
  void search_whenFilteringByCodeVariants_shouldReturnRelevantIssues() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setCodeVariants(asList("variant1", "variant2")),
      newDoc("I2", project.uuid(), file).setCodeVariants(singletonList("variant2")),
      newDoc("I3", project.uuid(), file).setCodeVariants(singletonList("variant3")),
      newDoc("I4", project.uuid(), file));

    assertThatSearchReturnsOnly(IssueQuery.builder().codeVariants(singletonList("variant2")), "I1", "I2");
  }

  @Test
  void search_whenFilteringBySoftwareQualities_shouldReturnRelevantIssues() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, HIGH,
        SECURITY, org.sonar.api.issue.impact.Severity.LOW,
        RELIABILITY, org.sonar.api.issue.impact.Severity.MEDIUM)),

      newDoc("I2", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW,
        SECURITY, org.sonar.api.issue.impact.Severity.LOW)),
      newDoc("I3", project.uuid(), file).setImpacts(Map.of(
        RELIABILITY, HIGH)),
      newDoc("I4", project.uuid(), file).setImpacts(Map.of(
        MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)));

    assertThatSearchReturnsOnly(IssueQuery.builder().impactSoftwareQualities(Set.of(MAINTAINABILITY.name())),
      "I1", "I2", "I4");

    assertThatSearchReturnsOnly(IssueQuery.builder().impactSoftwareQualities(Set.of(MAINTAINABILITY.name(), RELIABILITY.name())),
      "I1", "I2", "I3", "I4");

    assertThatSearchReturnsOnly(IssueQuery.builder().impactSeverities(Set.of(HIGH.name())),
      "I1", "I3");

    assertThatSearchReturnsOnly(IssueQuery.builder().impactSeverities(Set.of(org.sonar.api.issue.impact.Severity.LOW.name(), org.sonar.api.issue.impact.Severity.MEDIUM.name())),
      "I1", "I2", "I4");

    assertThatSearchReturnsOnly(IssueQuery.builder()
      .impactSoftwareQualities(Set.of(MAINTAINABILITY.name()))
        .impactSeverities(Set.of(HIGH.name())),
      "I1");

  }

  @Test
  void search_whenFilteringByCleanCodeAttributeCategory_shouldReturnRelevantIssues() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setCleanCodeAttributeCategory(ADAPTABLE.name()),
      newDoc("I2", project.uuid(), file).setCleanCodeAttributeCategory(ADAPTABLE.name()),
      newDoc("I3", project.uuid(), file).setCleanCodeAttributeCategory(CONSISTENT.name()),
      newDoc("I4", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I5", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I6", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I7", project.uuid(), file).setCleanCodeAttributeCategory(INTENTIONAL.name()),
      newDoc("I8", project.uuid(), file).setCleanCodeAttributeCategory(RESPONSIBLE.name()));

    assertThatSearchReturnsOnly(IssueQuery.builder().cleanCodeAttributesCategories(Set.of(ADAPTABLE.name())),
      "I1", "I2");

    assertThatSearchReturnsOnly(IssueQuery.builder().cleanCodeAttributesCategories(Set.of(CONSISTENT.name(), INTENTIONAL.name())),
      "I3", "I4", "I5", "I6", "I7");

    assertThatSearchReturnsOnly(IssueQuery.builder().cleanCodeAttributesCategories(
      Set.of(CONSISTENT.name(), INTENTIONAL.name(), RESPONSIBLE.name(), ADAPTABLE.name())),
      "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8");
  }

  @Test
  void search_whenFilteringByIssueStatus_shouldReturnRelevantIssues() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto file = newFileDto(project);

    indexIssues(
      newDoc("I1", project.uuid(), file).setIssueStatus(IssueStatus.CONFIRMED.name()),
      newDoc("I2", project.uuid(), file).setIssueStatus(IssueStatus.FIXED.name()),
      newDoc("I3", project.uuid(), file).setIssueStatus(IssueStatus.OPEN.name()),
      newDoc("I4", project.uuid(), file).setIssueStatus(IssueStatus.OPEN.name()),
      newDoc("I5", project.uuid(), file).setIssueStatus(IssueStatus.ACCEPTED.name()),
      newDoc("I6", project.uuid(), file).setIssueStatus(IssueStatus.ACCEPTED.name()),
      newDoc("I7", project.uuid(), file).setIssueStatus(IssueStatus.ACCEPTED.name()),
      newDoc("I8", project.uuid(), file).setIssueStatus(IssueStatus.FALSE_POSITIVE.name()),
      newDoc("I9", project.uuid(), file).setIssueStatus(IssueStatus.FALSE_POSITIVE.name()));

    assertThatSearchReturnsOnly(IssueQuery.builder().issueStatuses(Set.of(IssueStatus.CONFIRMED.name(), IssueStatus.OPEN.name())),
      "I1", "I3", "I4");

    assertThatSearchReturnsOnly(IssueQuery.builder().issueStatuses(Set.of(IssueStatus.FALSE_POSITIVE.name(), IssueStatus.ACCEPTED.name())),
      "I5", "I6", "I7", "I8", "I9");

    assertThatSearchReturnsOnly(IssueQuery.builder().issueStatuses(Set.of(IssueStatus.FIXED.name())),
      "I2");
  }

  private void indexView(String viewUuid, List<String> projectBranchUuids) {
    viewIndexer.index(new ViewDoc().setUuid(viewUuid).setProjectBranchUuids(projectBranchUuids));
  }
}
