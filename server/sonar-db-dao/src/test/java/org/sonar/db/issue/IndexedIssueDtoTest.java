/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.issue;

import java.util.Set;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.issue.status.IssueStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

public class IndexedIssueDtoTest {

  @Test
  public void settersGetters_shouldSetAndGetValues() {
    IndexedIssueDto indexedIssueDto = new IndexedIssueDto()
      .setIssueKey("issueKey")
      .setAssignee("assignee")
      .setAuthorLogin("authorLogin")
      .setStatus("status")
      .setNewCodeReferenceIssue(true)
      .setCleanCodeAttribute("cleanCodeAttribute")
      .setRuleCleanCodeAttribute("ruleCleanCodeAttribute")
      .setCodeVariants("codeVariants")
      .setSecurityStandards("securityStandards")
      .setComponentUuid("componentUuid")
      .setIssueCloseDate(1L)
      .setIssueCreationDate(2L)
      .setIssueUpdateDate(3L)
      .setEffort(4L)
      .setIsMain(true)
      .setLanguage("language")
      .setLine(5)
      .setPath("path")
      .setProjectUuid("projectUuid")
      .setQualifier("qualifier")
      .setResolution("resolution")
      .setRuleUuid("ruleUuid")
      .setScope("scope")
      .setSeverity("severity")
      .setTags("tags")
      .setIssueType(6)
      .setBranchUuid("branchUuid");

    indexedIssueDto.getImpacts().add(new ImpactDto().setSoftwareQuality(SoftwareQuality.SECURITY).setSeverity(Severity.HIGH));
    indexedIssueDto.getRuleDefaultImpacts().add(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(Severity.MEDIUM));

    assertThat(indexedIssueDto)
      .extracting(IndexedIssueDto::getIssueKey, IndexedIssueDto::getAssignee, IndexedIssueDto::getAuthorLogin, IndexedIssueDto::getStatus,
        IndexedIssueDto::isNewCodeReferenceIssue, IndexedIssueDto::getCleanCodeAttribute, IndexedIssueDto::getRuleCleanCodeAttribute, IndexedIssueDto::getCodeVariants,
        IndexedIssueDto::getSecurityStandards, IndexedIssueDto::getComponentUuid, IndexedIssueDto::getIssueCloseDate, IndexedIssueDto::getIssueCreationDate,
        IndexedIssueDto::getIssueUpdateDate, IndexedIssueDto::getEffort, IndexedIssueDto::isMain, IndexedIssueDto::getLanguage, IndexedIssueDto::getLine,
        IndexedIssueDto::getPath, IndexedIssueDto::getProjectUuid, IndexedIssueDto::getQualifier, IndexedIssueDto::getResolution,
        IndexedIssueDto::getRuleUuid, IndexedIssueDto::getScope, IndexedIssueDto::getSeverity, IndexedIssueDto::getTags, IndexedIssueDto::getIssueType,
        IndexedIssueDto::getBranchUuid)
      .containsExactly("issueKey", "assignee", "authorLogin", "status", true, "cleanCodeAttribute", "ruleCleanCodeAttribute", "codeVariants", "securityStandards",
        "componentUuid", 1L, 2L, 3L, 4L, true, "language", 5, "path", "projectUuid", "qualifier", "resolution", "ruleUuid",
        "scope", "severity", "tags", 6, "branchUuid");

    assertThat(indexedIssueDto.getImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactly(tuple(SoftwareQuality.SECURITY, Severity.HIGH));

    assertThat(indexedIssueDto.getRuleDefaultImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactly(tuple(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM));

    assertThat(indexedIssueDto.getEffectiveImpacts())
      .containsEntry(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM)
      .containsEntry(SoftwareQuality.SECURITY, Severity.HIGH);
  }

  @Test
  public void getIssueStatus_shouldReturnIssueStatusFromStatusAndResolution() {
    IndexedIssueDto issue1 = new IndexedIssueDto().setStatus(Issue.STATUS_OPEN);
    IndexedIssueDto issue2 = new IndexedIssueDto().setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_WONT_FIX);
    IndexedIssueDto issue3 = new IndexedIssueDto().setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED);

    assertThat(Set.of(issue1, issue2, issue3)).extracting(IndexedIssueDto::getIssueStatus)
      .containsExactlyInAnyOrder(IssueStatus.OPEN.name(), IssueStatus.ACCEPTED.name(), IssueStatus.FIXED.name());
  }

}
