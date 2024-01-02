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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class ProjectTrackerBaseLazyInputIT {

  private static final Date ANALYSIS_DATE = parseDate("2016-06-01");

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule().setAnalysisDate(ANALYSIS_DATE);
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ActiveRulesHolderRule activeRulesHolderRule = new ActiveRulesHolderRule();
  @Rule
  public RuleRepositoryRule ruleRepositoryRule = new RuleRepositoryRule();

  private DbClient dbClient = dbTester.getDbClient();
  private ProjectTrackerBaseLazyInput underTest;
  private RuleDto rule;
  private ComponentDto rootProjectDto;
  private ComponentIssuesLoader issuesLoader = new ComponentIssuesLoader(dbTester.getDbClient(), ruleRepositoryRule, activeRulesHolderRule, new MapSettings().asConfig(),
    System2.INSTANCE, mock(IssueChangesToDeleteRepository.class));

  @Before
  public void prepare() {
    rule = dbTester.rules().insert();
    ruleRepositoryRule.add(rule.getKey());
    rootProjectDto = dbTester.components().insertPublicProject().getMainBranchComponent();
    ReportComponent rootProject = ReportComponent.builder(Component.Type.FILE, 1)
      .setKey(rootProjectDto.getKey())
      .setUuid(rootProjectDto.uuid()).build();
    underTest = new ProjectTrackerBaseLazyInput(dbClient, issuesLoader, rootProject);
  }

  @Test
  public void return_only_open_project_issues_if_no_folders() {
    ComponentDto file = dbTester.components().insertComponent(newFileDto(rootProjectDto));
    IssueDto openIssueOnProject = dbTester.issues().insert(rule, rootProjectDto, rootProjectDto, i -> i.setStatus("OPEN").setResolution(null));
    IssueDto closedIssueOnProject = dbTester.issues().insert(rule, rootProjectDto, rootProjectDto, i -> i.setStatus("CLOSED").setResolution("FIXED"));
    IssueDto openIssue1OnFile = dbTester.issues().insert(rule, rootProjectDto, file, i -> i.setStatus("OPEN").setResolution(null));

    assertThat(underTest.loadIssues()).extracting(DefaultIssue::key).containsOnly(openIssueOnProject.getKey());
  }
}
