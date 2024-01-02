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
package org.sonar.db.project;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.ibatis.cursor.Cursor;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ProjectExportDaoIT {

  private final System2 system2 = new AlwaysIncreasingSystem2(1000L);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final ProjectExportDao projectExportDao = new ProjectExportDao();

  @Test
  public void selectBranchesForExport_shouldOnlyReturnBranchExcludedFromPurge() {
    ProjectData projectData = db.components().insertPrivateProject("project-uuid");
    BranchDto branchExcludedFromPurge = db.components().insertProjectBranch(projectData.getProjectDto(),
      branch -> branch.setExcludeFromPurge(true));
    db.components().insertProjectBranch(projectData.getProjectDto(),
      branch -> branch.setBranchType(BranchType.PULL_REQUEST).setExcludeFromPurge(true));
    db.components().insertProjectBranch(projectData.getProjectDto(),
      branch -> branch.setBranchType(BranchType.PULL_REQUEST));
    db.components().insertProjectBranch(projectData.getProjectDto());

    List<BranchDto> exportedBranch = projectExportDao.selectBranchesForExport(db.getSession(), "project-uuid");

    assertThat(exportedBranch)
      .hasSize(2)
      .containsOnly(projectData.getMainBranchDto(), branchExcludedFromPurge);
  }

  @Test
  public void selectPropertiesForExport_shouldOnlyReturnProjectPropertiesNotLinkedToUser() {
    db.components().insertPrivateProject("project-uuid");
    PropertyDto userProjectProperty = new PropertyDto().setKey("userProjectProperty").setEntityUuid("project-uuid").setUserUuid("user-uuid");
    PropertyDto globalProperty = new PropertyDto().setKey("globalProperty");
    PropertyDto projectProperty = new PropertyDto().setKey("projectProperty").setEntityUuid("project-uuid");
    db.properties().insertProperties(List.of(userProjectProperty, globalProperty, projectProperty), null, null, null, null);

    List<PropertyDto> exportedProperties = projectExportDao.selectPropertiesForExport(db.getSession(), "project-uuid");

    assertThat(exportedProperties)
      .hasSize(1)
      .extracting(PropertyDto::getKey).containsOnly("projectProperty");
  }

  @Test
  public void selectLinksForExport_shouldReturnLinkOfProject() {
    ProjectDto project = db.components().insertPrivateProject("project-uuid").getProjectDto();
    db.projectLinks().insertCustomLink(project, link -> link.setName("customLink").setHref("www.customLink.com"));
    db.projectLinks().insertProvidedLink(project, link -> link.setName("providedLink").setHref("www.providedLink.com"));
    ProjectDto otherProject = db.components().insertPrivateProject("another-project").getProjectDto();
    db.projectLinks().insertCustomLink(otherProject, link -> link.setName("customLink"));

    List<ProjectLinkDto> exportedLinks = projectExportDao.selectLinksForExport(db.getSession(), "project-uuid");

    assertThat(exportedLinks)
      .hasSize(2)
      .extracting("name", "href")
      .containsOnly(tuple("customLink", "www.customLink.com"), tuple("providedLink", "www.providedLink.com"));
  }

  @Test
  public void selectNewCodePeriodsForExport_shouldOnlyReturnGlobalNewCodePeriodOrNewCodePeriodOnBranchExcludedFromPurge() {
    ProjectDto project = db.components().insertPrivateProject("project-uuid").getProjectDto();
    BranchDto branchExcludedFromPurge = db.components().insertProjectBranch(project, branch -> branch.setExcludeFromPurge(true));
    BranchDto branchNotExcludedFromPurge = db.components().insertProjectBranch(project);
    db.newCodePeriods().insert(project.getUuid(), NewCodePeriodType.REFERENCE_BRANCH, "main");
    db.newCodePeriods().insert(project.getUuid(), branchExcludedFromPurge.getUuid(), NewCodePeriodType.PREVIOUS_VERSION, null);
    db.newCodePeriods().insert(project.getUuid(), branchNotExcludedFromPurge.getUuid(), NewCodePeriodType.NUMBER_OF_DAYS, "10");
    db.newCodePeriods().insert(NewCodePeriodType.SPECIFIC_ANALYSIS, "uuid");

    List<NewCodePeriodDto> exportedNewCodePeriods = projectExportDao.selectNewCodePeriodsForExport(db.getSession(), "project-uuid");

    assertThat(exportedNewCodePeriods)
      .hasSize(2)
      .extracting("type", "value")
      .containsOnly(tuple(NewCodePeriodType.REFERENCE_BRANCH, "main"), tuple(NewCodePeriodType.PREVIOUS_VERSION, null));
  }

  @Test
  public void scrollAdhocRulesForExport_shouldOnlyReturnAdHocRulesNotRemovedAndActiveOnProjectBranchesExcludedFromPurge() {
    ProjectData projectData = db.components().insertPrivateProject("project-uuid");
    ComponentDto file = db.components().insertFile(projectData.getMainBranchDto());
    ComponentDto mainBranchComponent = projectData.getMainBranchComponent();
    ComponentDto branchNotExcludedFromPurge = db.components().insertProjectBranch(mainBranchComponent);

    RuleDto externalRule = insertRule("externalRule", true, false);
    db.issues().insertIssue(externalRule, mainBranchComponent, file, issueDto -> issueDto.setKee("issue-externalRule"));
    RuleDto adHocRule1 = insertRule("adHocRule1", false, true);
    db.issues().insertIssue(adHocRule1, mainBranchComponent, file, issueDto -> issueDto.setKee("issue-adHocRule1"));
    RuleDto adHocRule2 = insertRule("adHocRule2", false, true);
    db.issues().insertIssue(adHocRule2, branchNotExcludedFromPurge, file, issueDto -> issueDto.setKee("issue-adHocRule2"));
    RuleDto adHocRule3 = insertRule("adHocRule3", false, true, RuleStatus.REMOVED);
    db.issues().insertIssue(adHocRule3, mainBranchComponent, file, issueDto -> issueDto.setKee("issue-adHocRule3"));
    RuleDto adHocRule4 = insertRule("adHocRule4", false, true);
    db.issues().insertIssue(adHocRule4, mainBranchComponent, file, issueDto -> issueDto.setKee("issue-adHocRule4").setStatus(Issue.STATUS_CLOSED));
    RuleDto standardRule = insertRule("standardRule", false, false);
    db.issues().insertIssue(standardRule, mainBranchComponent, file, issueDto -> issueDto.setKee("issue-standardRule"));

    Cursor<RuleDto> ruleDtos = projectExportDao.scrollAdhocRulesForExport(db.getSession(), "project-uuid");
    Set<RuleDto> ruleDtoSet = toSet(ruleDtos);

    assertThat(ruleDtoSet)
      .hasSize(1)
      .extracting("uuid")
      .containsOnly(adHocRule1.getUuid());

  }

  @Test
  public void scrollIssueForExport_shouldOnlyReturnIssueWithRulesNotRemovedAndActiveOnProjectBranchesExcludedFromPurge() {
    ProjectData projectData = db.components().insertPrivateProject("project-uuid");
    ComponentDto file = db.components().insertFile(projectData.getMainBranchDto());
    ComponentDto mainBranchComponent = projectData.getMainBranchComponent();
    ComponentDto branchNotExcludedFromPurge = db.components().insertProjectBranch(mainBranchComponent);

    RuleDto rule1 = db.rules().insertRule(ruleDto -> ruleDto.setUuid("rule1"));
    db.issues().insertIssue(rule1, mainBranchComponent, file, issueDto -> issueDto.setKee("issue1"));
    db.issues().insertIssue(rule1, branchNotExcludedFromPurge, file, issueDto -> issueDto.setKee("issue2"));
    RuleDto rule2 = db.rules().insertRule(ruleDto -> ruleDto.setUuid("rule2").setStatus(RuleStatus.REMOVED));
    db.issues().insertIssue(rule2, mainBranchComponent, file, issueDto -> issueDto.setKee("issue3"));
    db.rules().insertRule(ruleDto -> ruleDto.setUuid("rule3"));
    db.issues().insertIssue(rule2, mainBranchComponent, file, issueDto -> issueDto.setKee("issue5").setStatus(Issue.STATUS_CLOSED));

    Cursor<IssueDto> issueDtos = projectExportDao.scrollIssueForExport(db.getSession(), "project-uuid");
    Set<IssueDto> issuesSet = toSet(issueDtos);

    assertThat(issuesSet)
      .hasSize(1)
      .extracting("kee")
      .containsOnly("issue1");

  }

  private RuleDto insertRule(String ruleName, boolean isExternal, boolean isAdHoc) {
    return insertRule(ruleName, isExternal, isAdHoc, RuleStatus.READY);
  }

  private RuleDto insertRule(String ruleName, boolean isExternal, boolean isAdHoc, RuleStatus ruleStatus) {
    RuleKey ruleKey = RuleKey.of("plugin1", ruleName);
    db.rules().insertIssueRule(ProjectExportDaoIT.initRuleConsumer(ruleKey, isExternal, isAdHoc, ruleStatus));
    return db.getDbClient().ruleDao().selectByKey(db.getSession(), ruleKey)
      .orElseThrow(() -> new RuntimeException("insertAdHocRule failed"));
  }

  private static Consumer<RuleDto> initRuleConsumer(RuleKey ruleKey, boolean isExternal, boolean isAdHoc, RuleStatus ruleStatus) {
    return rule -> {
      rule
        .setName(ruleKey.rule())
        .setIsExternal(isExternal)
        .setIsAdHoc(isAdHoc)
        .setRuleKey(ruleKey)
        .setPluginKey("pluginKey" + RandomStringUtils.randomAlphanumeric(10))
        .setStatus(ruleStatus);
      if (isAdHoc) {
        rule.setAdHocName("ad_hoc_rule" + RandomStringUtils.randomAlphabetic(10))
          .setAdHocType(RuleType.VULNERABILITY)
          .setAdHocSeverity(Severity.CRITICAL)
          .setAdHocDescription("ad hoc description: " + RandomStringUtils.randomAlphanumeric(100));
      }
    };
  }

  private static <T> Set<T> toSet(Cursor<T> ruleDtos) {
    Iterator<T> ruleIterator = ruleDtos.iterator();
    return Stream.generate(() -> null)
      .takeWhile(x -> ruleIterator.hasNext())
      .map(n -> ruleIterator.next())
      .collect(Collectors.toSet());
  }

}
