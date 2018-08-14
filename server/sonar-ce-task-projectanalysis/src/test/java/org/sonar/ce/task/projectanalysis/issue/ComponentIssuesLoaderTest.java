/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.addDays;

public class ComponentIssuesLoaderTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private ComponentIssuesLoader underTest = new ComponentIssuesLoader(dbClient,
    null /* not used in loadClosedIssues */, null /* not used in loadClosedIssues */);

  @Test
  public void loadClosedIssues_returns_single_DefaultIssue_by_issue_based_on_first_row() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = dbTester.rules().insert(t -> t.setType(RuleType.CODE_SMELL));
    IssueDto issue = dbTester.issues().insert(rule, project, file, t -> t.setStatus(Issue.STATUS_CLOSED).setIsFromHotspot(false));
    Date creationDate = new Date();
    dbTester.issues().insertFieldDiffs(issue, new FieldDiffs().setCreationDate(addDays(creationDate, -5)).setDiff("line", 10, ""));
    dbTester.issues().insertFieldDiffs(issue, new FieldDiffs().setCreationDate(creationDate).setDiff("line", 20, ""));
    dbTester.issues().insertFieldDiffs(issue, new FieldDiffs().setCreationDate(addDays(creationDate, -10)).setDiff("line", 30, ""));

    List<DefaultIssue> defaultIssues = underTest.loadClosedIssues(file.uuid());

    assertThat(defaultIssues).hasSize(1);
    assertThat(defaultIssues.iterator().next().getLine()).isEqualTo(20);
  }

  @Test
  public void loadClosedIssues_returns_single_DefaultIssue_ignoring_lines_without_old_values() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(project));
    RuleDefinitionDto rule = dbTester.rules().insert(t -> t.setType(RuleType.CODE_SMELL));
    IssueDto issue = dbTester.issues().insert(rule, project, file, t -> t.setStatus(Issue.STATUS_CLOSED).setIsFromHotspot(false));
    Date creationDate = new Date();
    dbTester.issues().insertFieldDiffs(issue, new FieldDiffs().setCreationDate(addDays(creationDate, -5)).setDiff("line", null, ""));
    dbTester.issues().insertFieldDiffs(issue, new FieldDiffs().setCreationDate(creationDate).setDiff("line", 20, null)); // if new value is null, neither old nor new is stored in DB
    dbTester.issues().insertFieldDiffs(issue, new FieldDiffs().setCreationDate(addDays(creationDate, -10)).setDiff("line", 30, ""));

    List<DefaultIssue> defaultIssues = underTest.loadClosedIssues(file.uuid());

    assertThat(defaultIssues).hasSize(1);
    assertThat(defaultIssues.iterator().next().getLine()).isEqualTo(30);
  }
}
