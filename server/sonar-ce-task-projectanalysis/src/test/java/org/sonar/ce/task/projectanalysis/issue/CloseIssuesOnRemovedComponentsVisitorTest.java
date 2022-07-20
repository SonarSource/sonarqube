/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.pushevent.PushEvent;
import org.sonar.ce.task.projectanalysis.pushevent.PushEventRepository;
import org.sonar.ce.task.projectanalysis.pushevent.TaintVulnerabilityClosed;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.issue.TaintChecker;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;

public class CloseIssuesOnRemovedComponentsVisitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  ComponentIssuesLoader issuesLoader = mock(ComponentIssuesLoader.class);
  ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues = mock(ComponentsWithUnprocessedIssues.class);
  IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);
  ProtoIssueCache protoIssueCache;
  VisitorsCrawler underTest;
  PushEventRepository pushEventRepository = mock(PushEventRepository.class);
  TaintChecker taintChecker = mock(TaintChecker.class);

  @Before
  public void setUp() throws Exception {
    protoIssueCache = new ProtoIssueCache(temp.newFile(), System2.INSTANCE);
    underTest = new VisitorsCrawler(
      Arrays.asList(new CloseIssuesOnRemovedComponentsVisitor(issuesLoader, componentsWithUnprocessedIssues,
        protoIssueCache, issueLifecycle, pushEventRepository, taintChecker)));
  }

  @Test
  public void close_issue() {
    String fileUuid = "FILE1";
    String issueUuid = "ABCD";

    when(componentsWithUnprocessedIssues.getUuids()).thenReturn(newHashSet(fileUuid));
    DefaultIssue issue = new DefaultIssue().setKey(issueUuid).setType(RuleType.BUG).setCreationDate(new Date())
      .setComponentKey("c").setProjectUuid("u").setProjectKey("k").setRuleKey(RuleKey.of("r", "r")).setStatus("OPEN");
    when(issuesLoader.loadOpenIssues(fileUuid)).thenReturn(Collections.singletonList(issue));
    when(taintChecker.isTaintVulnerability(any())).thenReturn(false);

    underTest.visit(ReportComponent.builder(PROJECT, 1).build());

    verify(issueLifecycle).doAutomaticTransition(issue);
    verifyNoInteractions(pushEventRepository);
    CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse();
    assertThat(issues.hasNext()).isTrue();

    DefaultIssue result = issues.next();
    assertThat(result.key()).isEqualTo(issueUuid);
    assertThat(result.isBeingClosed()).isTrue();
    assertThat(result.isOnDisabledRule()).isFalse();
  }

  @Test
  public void close_taint_vulnerability() {
    String fileUuid = "FILE1";
    String issueUuid = "ABCD";

    when(componentsWithUnprocessedIssues.getUuids()).thenReturn(newHashSet(fileUuid));
    DefaultIssue issue = new DefaultIssue().setKey(issueUuid).setType(RuleType.BUG).setCreationDate(new Date())
      .setComponentKey("c").setProjectUuid("u").setProjectKey("k").setRuleKey(RuleKey.of("r", "r")).setStatus("OPEN");
    when(issuesLoader.loadOpenIssues(fileUuid)).thenReturn(Collections.singletonList(issue));
    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    underTest.visit(ReportComponent.builder(PROJECT, 1).build());

    verify(issueLifecycle).doAutomaticTransition(issue);

    ArgumentCaptor<PushEvent<TaintVulnerabilityClosed>> pushEventCaptor = ArgumentCaptor.forClass(PushEvent.class);
    verify(pushEventRepository).add(pushEventCaptor.capture());
    PushEvent<TaintVulnerabilityClosed> pushEvent = pushEventCaptor.getValue();
    assertThat(pushEvent.getName()).isEqualTo("TaintVulnerabilityClosed");

    CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse();
    assertThat(issues.hasNext()).isTrue();

    DefaultIssue result = issues.next();
    assertThat(result.key()).isEqualTo(issueUuid);
    assertThat(result.isBeingClosed()).isTrue();
    assertThat(result.isOnDisabledRule()).isFalse();
  }

  @Test
  public void nothing_to_do_when_no_uuid_in_queue() {
    when(componentsWithUnprocessedIssues.getUuids()).thenReturn(Collections.emptySet());

    underTest.visit(ReportComponent.builder(PROJECT, 1).build());

    verifyNoInteractions(issueLifecycle);
    CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse();
    assertThat(issues.hasNext()).isFalse();
  }

  @Test
  public void do_nothing_on_directory() {
    underTest.visit(ReportComponent.builder(DIRECTORY, 1).build());

    verifyNoInteractions(issueLifecycle);
    CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse();
    assertThat(issues.hasNext()).isFalse();
  }

  @Test
  public void do_nothing_on_file() {
    underTest.visit(ReportComponent.builder(FILE, 1).build());

    verifyNoInteractions(issueLifecycle);
    CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse();
    assertThat(issues.hasNext()).isFalse();
  }
}
