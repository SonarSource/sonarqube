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
package org.sonar.batch.protocol.output.issue;

import org.junit.Test;

import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;

public class ReportIssueTest {

  @Test
  public void testGetterSetter() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    ReportIssue issue = new ReportIssue()
      .setActionPlanKey("plan")
      .setAssignee("assignee")
      .setAuthorLogin("author")
      .setChanged(true)
      .setChecksum("checksum")
      .setDebt(3L)
      .setDiffFields("diff")
      .setEffortToFix(2.0)
      .setAttributes("attributes")
      .setCloseDate(sdf.parse("11/12/2012"))
      .setCreationDate(sdf.parse("12/12/2012"))
      .setUpdateDate(sdf.parse("13/12/2012"))
      .setKey("key")
      .setLine(3)
      .setManualSeverity(true)
      .setMessage("message")
      .setNew(true)
      .setReporter("reporter")
      .setResolution("resolution")
      .setResourceBatchId(4L)
      .setRuleKey("repo", "rule")
      .setSelectedAt(234L)
      .setSeverity("severity")
      .setStatus("status");

    assertThat(issue.actionPlanKey()).isEqualTo("plan");
    assertThat(issue.assignee()).isEqualTo("assignee");
    assertThat(issue.authorLogin()).isEqualTo("author");
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.checksum()).isEqualTo("checksum");
    assertThat(issue.debt()).isEqualTo(3L);
    assertThat(issue.diffFields()).isEqualTo("diff");
    assertThat(issue.effortToFix()).isEqualTo(2.0);
    assertThat(issue.issueAttributes()).isEqualTo("attributes");
    assertThat(issue.closeDate()).isEqualTo(sdf.parse("11/12/2012"));
    assertThat(issue.creationDate()).isEqualTo(sdf.parse("12/12/2012"));
    assertThat(issue.updateDate()).isEqualTo(sdf.parse("13/12/2012"));
    assertThat(issue.key()).isEqualTo("key");
    assertThat(issue.line()).isEqualTo(3);
    assertThat(issue.isManualSeverity()).isTrue();
    assertThat(issue.message()).isEqualTo("message");
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.reporter()).isEqualTo("reporter");
    assertThat(issue.resolution()).isEqualTo("resolution");
    assertThat(issue.resourceBatchId()).isEqualTo(4L);
    assertThat(issue.ruleRepo()).isEqualTo("repo");
    assertThat(issue.ruleKey()).isEqualTo("rule");
    assertThat(issue.selectedAt()).isEqualTo(234L);
    assertThat(issue.severity()).isEqualTo("severity");
    assertThat(issue.status()).isEqualTo("status");
  }

}
