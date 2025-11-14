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
package org.sonar.ce.task.projectanalysis.step;

import java.io.IOException;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.issue.ChangedIssuesRepository;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.rule.Severity.BLOCKER;

public class LoadChangedIssuesStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final PeriodHolder periodHolder = mock(PeriodHolder.class);
  private ProtoIssueCache protoIssueCache;

  private final ChangedIssuesRepository changedIssuesRepository = mock(ChangedIssuesRepository.class);

  private LoadChangedIssuesStep underTest;

  @Before
  public void before() throws IOException {
    protoIssueCache = new ProtoIssueCache(temp.newFile(), System2.INSTANCE);
    underTest = new LoadChangedIssuesStep(periodHolder, protoIssueCache, changedIssuesRepository);
  }

  @Test
  public void getDescription_shouldReturnDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Load changed issues for indexing");
  }

  @Test
  public void execute_whenIssueIsNew_shouldLoadIssue() {
    protoIssueCache.newAppender()
      .append(newDefaultIssue().setNew(true))
      .close();

    underTest.execute(mock(ComputationStep.Context.class));

    verify(changedIssuesRepository).addIssueKey("issueKey1");
  }

  @Test
  public void execute_whenIssueIssCopied_shouldLoadIssue() {
    protoIssueCache.newAppender()
      .append(newDefaultIssue().setCopied(true))
      .close();

    underTest.execute(mock(ComputationStep.Context.class));

    verify(changedIssuesRepository).addIssueKey("issueKey1");
  }

  @Test
  public void execute_whenIssueIsChanged_shouldLoadIssue() {
    protoIssueCache.newAppender()
      .append(newDefaultIssue().setChanged(true))
      .close();

    underTest.execute(mock(ComputationStep.Context.class));

    verify(changedIssuesRepository).addIssueKey("issueKey1");
  }

  @Test
  public void execute_whenIssueIsNoLongerNewCodeReferenceIssue_shouldLoadIssue() {
    when(periodHolder.hasPeriod()).thenReturn(true);
    when(periodHolder.getPeriod()).thenReturn(new Period("REFERENCE_BRANCH", null, null));

    protoIssueCache.newAppender()
      .append(newDefaultIssue()
        .setIsNoLongerNewCodeReferenceIssue(true)
        .setNew(false)
        .setCopied(false)
        .setChanged(false))
      .close();

    underTest.execute(mock(ComputationStep.Context.class));

    verify(changedIssuesRepository).addIssueKey("issueKey1");
  }

  @Test
  public void execute_whenIssueIsToBeMigratedAsNewCodeReferenceIssue_shouldLoadIssue() {
    when(periodHolder.hasPeriod()).thenReturn(true);
    when(periodHolder.getPeriod()).thenReturn(new Period("REFERENCE_BRANCH", null, null));

    protoIssueCache.newAppender()
      .append(newDefaultIssue()
        .setIsOnChangedLine(true)
        .setIsNewCodeReferenceIssue(false)
        .setIsNoLongerNewCodeReferenceIssue(false)
        .setNew(false)
        .setCopied(false)
        .setChanged(false))
      .close();

    underTest.execute(mock(ComputationStep.Context.class));

    verify(changedIssuesRepository).addIssueKey("issueKey1");
  }

  private static DefaultIssue newDefaultIssue() {
    return new DefaultIssue()
      .setKey("issueKey1")
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of("repo", "ruleKey1"))
      .setComponentUuid("fileUuid")
      .setComponentKey("fileKey")
      .setProjectUuid("projectUuid")
      .setProjectKey("projectKey")
      .setSeverity(BLOCKER)
      .setStatus(STATUS_OPEN)
      .setCreationDate(new Date())
      .setSelectedAt(1L)
      .addImpact(SoftwareQuality.SECURITY, Severity.MEDIUM);
  }

}
