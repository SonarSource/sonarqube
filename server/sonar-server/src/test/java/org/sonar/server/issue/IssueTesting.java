/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue;

import com.google.common.collect.Maps;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.rule.RuleTesting;

import static org.fest.assertions.Assertions.assertThat;

public class IssueTesting {

  /**
   * Full IssueDto used to feed database with fake data. Tests must not rely on the
   * field contents declared here. They should override the fields they need to test,
   * for example:
   * <pre>
   *   issueDao.insert(dbSession, IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FALSE_POSITIVE));
   * </pre>
   */
  public static IssueDto newDto(RuleDto rule, ComponentDto file, ComponentDto project) {
    return new IssueDto()
      .setKee(Uuids.create())
      .setRule(rule)
      .setComponent(file)
      .setProject(project)
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null)
      .setSeverity(Severity.MAJOR)
      .setDebt(10L)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setCreatedAt(1400000000000L)
      .setUpdatedAt(1400000000000L);
  }

  public static IssueDoc newDoc() {
    IssueDoc doc = new IssueDoc(Maps.<String, Object>newHashMap());
    doc.setKey("ABC");
    doc.setRuleKey(RuleTesting.XOO_X1.toString());
    doc.setActionPlanKey(null);
    doc.setReporter(null);
    doc.setAssignee("steve");
    doc.setAuthorLogin("roger");
    doc.setLanguage("xoo");
    doc.setComponentUuid("FILE_1");
    doc.setEffortToFix(3.14);
    doc.setFilePath("src/Foo.xoo");
    doc.setMessage("the message");
    doc.setModuleUuid("MODULE_1");
    doc.setModuleUuidPath("MODULE_1");
    doc.setProjectUuid("PROJECT_1");
    doc.setLine(42);
    doc.setAttributes(null);
    doc.setStatus(Issue.STATUS_OPEN);
    doc.setResolution(null);
    doc.setSeverity(Severity.MAJOR);
    doc.setDebt(10L);
    doc.setFuncCreationDate(DateUtils.parseDate("2014-09-04"));
    doc.setFuncUpdateDate(DateUtils.parseDate("2014-12-04"));
    doc.setFuncCloseDate(null);
    doc.setTechnicalUpdateDate(DateUtils.parseDate("2014-12-04"));
    return doc;
  }

  public static void assertIsEquivalent(IssueDto dto, IssueDoc issue) {
    assertThat(issue).isNotNull();
    assertThat(dto).isNotNull();

    assertThat(issue.key()).isEqualTo(dto.getKey());
    assertThat(issue.componentUuid()).isEqualTo(dto.getComponentUuid());
    assertThat(issue.moduleUuid()).isEqualTo(dto.getModuleUuid());
    assertThat(issue.projectUuid()).isEqualTo(dto.getProjectUuid());

    assertThat(issue.actionPlanKey()).isEqualTo(dto.getActionPlanKey());
    assertThat(issue.assignee()).isEqualTo(dto.getAssignee());
    assertThat(issue.authorLogin()).isEqualTo(dto.getAuthorLogin());
    assertThat(issue.closeDate()).isEqualTo(dto.getIssueCloseDate());
    assertThat(issue.effortToFix()).isEqualTo(dto.getEffortToFix());
    assertThat(issue.resolution()).isEqualTo(dto.getResolution());
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of(dto.getRuleRepo(), dto.getRule()));
    assertThat(issue.line()).isEqualTo(dto.getLine());
    assertThat(issue.message()).isEqualTo(dto.getMessage());
    assertThat(issue.reporter()).isEqualTo(dto.getReporter());
    assertThat(issue.language()).isEqualTo(dto.getLanguage());
    assertThat(issue.status()).isEqualTo(dto.getStatus());
    assertThat(issue.severity()).isEqualTo(dto.getSeverity());
    assertThat(issue.filePath()).isEqualTo(dto.getFilePath());

    assertThat(issue.attributes()).isEqualTo(KeyValueFormat.parse(dto.getIssueAttributes()));

    assertThat(issue.creationDate()).isEqualTo(dto.getIssueCreationDate());
    assertThat(issue.updateDate()).isEqualTo(dto.getIssueUpdateDate());
    assertThat(issue.closeDate()).isEqualTo(dto.getIssueCloseDate());
  }
}
