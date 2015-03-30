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
package org.sonar.batch.issue.tracking;

import com.google.common.base.Charsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.java.api.JavaClass;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class IssueTrackingDecoratorTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  IssueTrackingDecorator decorator;
  IssueCache issueCache = mock(IssueCache.class, RETURNS_MOCKS);
  InitialOpenIssuesStack initialOpenIssues = mock(InitialOpenIssuesStack.class);
  IssueTracking tracking = mock(IssueTracking.class, RETURNS_MOCKS);
  ServerLineHashesLoader lastSnapshots = mock(ServerLineHashesLoader.class);
  IssueHandlers handlers = mock(IssueHandlers.class);
  IssueWorkflow workflow = mock(IssueWorkflow.class);
  IssueUpdater updater = mock(IssueUpdater.class);
  ResourcePerspectives perspectives = mock(ResourcePerspectives.class);
  RulesProfile profile = mock(RulesProfile.class);
  RuleFinder ruleFinder = mock(RuleFinder.class);
  InputPathCache inputPathCache = mock(InputPathCache.class);

  @Before
  public void init() {
    decorator = new IssueTrackingDecorator(
      issueCache,
      initialOpenIssues,
      tracking,
      lastSnapshots,
      handlers,
      workflow,
      updater,
      new Project("foo"),
      perspectives,
      profile,
      ruleFinder,
      inputPathCache);
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_be_executed_on_classes_not_methods() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(JavaClass.create("org.foo.Bar"), context);
    verifyZeroInteractions(context, issueCache, tracking, handlers, workflow);
  }

  @Test
  public void should_process_open_issues() throws Exception {
    Resource file = File.create("Action.java").setEffectiveKey("struts:Action.java").setId(123);
    final DefaultIssue issue = new DefaultIssue();

    // INPUT : one issue, no open issues during previous scan, no filtering
    when(issueCache.byComponent("struts:Action.java")).thenReturn(Arrays.asList(issue));
    List<ServerIssue> dbIssues = Collections.emptyList();
    when(initialOpenIssues.selectAndRemoveIssues("struts:Action.java")).thenReturn(dbIssues);
    when(inputPathCache.getFile("foo", "Action.java")).thenReturn(mock(DefaultInputFile.class));
    decorator.doDecorate(file);

    // Apply filters, track, apply transitions, notify extensions then update cache
    verify(tracking).track(isA(SourceHashHolder.class), eq(dbIssues), argThat(new ArgumentMatcher<Collection<DefaultIssue>>() {
      @Override
      public boolean matches(Object o) {
        List<DefaultIssue> issues = (List<DefaultIssue>) o;
        return issues.size() == 1 && issues.get(0) == issue;
      }
    }));
    verify(workflow).doAutomaticTransition(eq(issue), any(IssueChangeContext.class));
    verify(handlers).execute(eq(issue), any(IssueChangeContext.class));
    verify(issueCache).put(issue);
  }

  @Test
  public void should_register_unmatched_issues_as_end_of_life() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed
    Resource file = File.create("Action.java").setEffectiveKey("struts:Action.java").setId(123);

    // INPUT : one issue existing during previous scan
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setResolution(null).setStatus("OPEN").setRuleKey("squid", "AvoidCycle"));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);
    when(inputPathCache.getFile("foo", "Action.java")).thenReturn(mock(DefaultInputFile.class));

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isTrue();
  }

  @Test
  public void manual_issues_should_be_moved_if_matching_line_found() throws Exception {
    // INPUT : one issue existing during previous scan
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(6).setStatus("OPEN").setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance"));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String originalSource = "public interface Action {\n"
      + "   void method1();\n"
      + "   void method2();\n"
      + "   void method3();\n"
      + "   void method4();\n"
      + "   void method5();\n" // Original issue here
      + "}";
    String newSource = "public interface Action {\n"
      + "   void method5();\n" // New issue here
      + "   void method1();\n"
      + "   void method2();\n"
      + "   void method3();\n"
      + "   void method4();\n"
      + "}";
    Resource file = mockHashes(originalSource, newSource);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.line()).isEqualTo(2);
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isFalse();
    assertThat(issue.isOnDisabledRule()).isFalse();
  }

  private Resource mockHashes(String originalSource, String newSource) throws IOException {
    DefaultInputFile inputFile = mock(DefaultInputFile.class);
    java.io.File f = temp.newFile();
    when(inputFile.path()).thenReturn(f.toPath());
    when(inputFile.file()).thenReturn(f);
    when(inputFile.charset()).thenReturn(Charsets.UTF_8);
    when(inputFile.lines()).thenReturn(StringUtils.countMatches(newSource, "\n") + 1);
    FileUtils.write(f, newSource, Charsets.UTF_8);
    when(inputFile.key()).thenReturn("foo:Action.java");
    when(inputPathCache.getFile("foo", "Action.java")).thenReturn(inputFile);
    when(lastSnapshots.getLineHashes("foo:Action.java")).thenReturn(computeHexHashes(originalSource));
    Resource file = File.create("Action.java");
    return file;
  }

  @Test
  public void manual_issues_should_be_untouched_if_already_closed() throws Exception {

    // INPUT : one issue existing during previous scan
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(1).setStatus("CLOSED").setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance"));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String originalSource = "public interface Action {}";
    Resource file = mockHashes(originalSource, originalSource);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.line()).isEqualTo(1);
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isFalse();
    assertThat(issue.isOnDisabledRule()).isFalse();
    assertThat(issue.status()).isEqualTo("CLOSED");
  }

  @Test
  public void manual_issues_should_be_untouched_if_line_is_null() throws Exception {

    // INPUT : one issue existing during previous scan
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(null).setStatus("OPEN").setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance"));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String originalSource = "public interface Action {}";
    Resource file = mockHashes(originalSource, originalSource);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.line()).isEqualTo(null);
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isFalse();
    assertThat(issue.isOnDisabledRule()).isFalse();
    assertThat(issue.status()).isEqualTo("OPEN");
  }

  @Test
  public void manual_issues_should_be_kept_if_matching_line_not_found() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed

    // INPUT : one issue existing during previous scan
    final int issueOnLine = 6;
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(issueOnLine).setStatus("OPEN")
      .setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance"));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String originalSource = "public interface Action {\n"
      + "   void method1();\n"
      + "   void method2();\n"
      + "   void method3();\n"
      + "   void method4();\n"
      + "   void method5();\n" // Original issue here
      + "}";
    String newSource = "public interface Action {\n"
      + "   void method1();\n"
      + "   void method2();\n"
      + "   void method3();\n"
      + "   void method4();\n"
      + "   void method6();\n" // Poof, no method5 anymore
      + "}";

    Resource file = mockHashes(originalSource, newSource);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.line()).isEqualTo(issueOnLine);
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isFalse();
    assertThat(issue.isOnDisabledRule()).isFalse();
  }

  @Test
  public void manual_issues_should_be_kept_if_multiple_matching_lines_found() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed

    // INPUT : one issue existing during previous scan
    final int issueOnLine = 3;
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(issueOnLine).setStatus("OPEN")
      .setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance"));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String originalSource = "public class Action {\n"
      + "   void method1() {\n"
      + "     notify();\n" // initial issue
      + "   }\n"
      + "}";
    String newSource = "public class Action {\n"
      + "   \n"
      + "   void method1() {\n" // new issue will appear here
      + "     notify();\n"
      + "   }\n"
      + "   void method2() {\n"
      + "     notify();\n"
      + "   }\n"
      + "}";
    Resource file = mockHashes(originalSource, newSource);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.line()).isEqualTo(issueOnLine);
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isFalse();
    assertThat(issue.isOnDisabledRule()).isFalse();
  }

  @Test
  public void manual_issues_should_be_closed_if_manual_rule_is_removed() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed

    // INPUT : one issue existing during previous scan
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(1).setStatus("OPEN").setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(new Rule("manual", "Performance").setStatus(Rule.STATUS_REMOVED));

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String source = "public interface Action {}";
    Resource file = mockHashes(source, source);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isTrue();
    assertThat(issue.isOnDisabledRule()).isTrue();
  }

  @Test
  public void manual_issues_should_be_closed_if_manual_rule_is_not_found() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed

    // INPUT : one issue existing during previous scan
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(1).setStatus("OPEN").setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(null);

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String source = "public interface Action {}";
    Resource file = mockHashes(source, source);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isTrue();
    assertThat(issue.isOnDisabledRule()).isTrue();
  }

  @Test
  public void manual_issues_should_be_closed_if_new_source_is_shorter() throws Exception {
    // "Unmatched" issues existed in previous scan but not in current one -> they have to be closed

    // INPUT : one issue existing during previous scan
    ServerIssue unmatchedIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setReporter("freddy").setLine(6).setStatus("OPEN").setRuleKey("manual", "Performance"));
    when(ruleFinder.findByKey(RuleKey.of("manual", "Performance"))).thenReturn(null);

    IssueTrackingResult trackingResult = new IssueTrackingResult();
    trackingResult.addUnmatched(unmatchedIssue);

    String originalSource = "public interface Action {\n"
      + "   void method1();\n"
      + "   void method2();\n"
      + "   void method3();\n"
      + "   void method4();\n"
      + "   void method5();\n"
      + "}";
    String newSource = "public interface Action {\n"
      + "   void method1();\n"
      + "   void method2();\n"
      + "}";
    Resource file = mockHashes(originalSource, newSource);

    when(tracking.track(isA(SourceHashHolder.class), anyCollection(), anyCollection())).thenReturn(trackingResult);

    decorator.doDecorate(file);

    verify(workflow, times(1)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(1)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));

    ArgumentCaptor<DefaultIssue> argument = ArgumentCaptor.forClass(DefaultIssue.class);
    verify(issueCache).put(argument.capture());

    DefaultIssue issue = argument.getValue();
    verify(updater).setResolution(eq(issue), eq(Issue.RESOLUTION_REMOVED), any(IssueChangeContext.class));
    verify(updater).setStatus(eq(issue), eq(Issue.STATUS_CLOSED), any(IssueChangeContext.class));

    assertThat(issue.key()).isEqualTo("ABCDE");
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isEndOfLife()).isTrue();
    assertThat(issue.isOnDisabledRule()).isTrue();
  }

  @Test
  public void should_register_issues_on_deleted_components() throws Exception {
    Project project = new Project("struts");
    DefaultIssue openIssue = new DefaultIssue();
    when(issueCache.byComponent("struts")).thenReturn(Arrays.asList(openIssue));
    IssueDto deadIssue = new IssueDto().setKee("ABCDE").setResolution(null).setStatus("OPEN").setRuleKey("squid", "AvoidCycle");
    when(initialOpenIssues.selectAllIssues()).thenReturn(Arrays.asList(deadIssue));

    decorator.doDecorate(project);

    // the dead issue must be closed -> apply automatic transition, notify handlers and add to cache
    verify(workflow, times(2)).doAutomaticTransition(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(handlers, times(2)).execute(any(DefaultIssue.class), any(IssueChangeContext.class));
    verify(issueCache, times(2)).put(any(DefaultIssue.class));

    verify(issueCache).put(argThat(new ArgumentMatcher<DefaultIssue>() {
      @Override
      public boolean matches(Object o) {
        DefaultIssue dead = (DefaultIssue) o;
        return "ABCDE".equals(dead.key()) && !dead.isNew() && dead.isEndOfLife();
      }
    }));
  }

  @Test
  public void merge_matched_issue() throws Exception {
    ServerIssue previousIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setResolution(null).setStatus("OPEN").setRuleKey("squid", "AvoidCycle")
      .setLine(10).setSeverity("MAJOR").setMessage("Message").setEffortToFix(1.5).setDebt(1L).setProjectKey("sample"));
    DefaultIssue issue = new DefaultIssue();

    IssueTrackingResult trackingResult = mock(IssueTrackingResult.class);
    when(trackingResult.matched()).thenReturn(newArrayList(issue));
    when(trackingResult.matching(eq(issue))).thenReturn(previousIssue);
    decorator.mergeMatched(trackingResult);

    verify(updater).setPastSeverity(eq(issue), eq("MAJOR"), any(IssueChangeContext.class));
    verify(updater).setPastLine(eq(issue), eq(10));
    verify(updater).setPastMessage(eq(issue), eq("Message"), any(IssueChangeContext.class));
    verify(updater).setPastEffortToFix(eq(issue), eq(1.5), any(IssueChangeContext.class));
    verify(updater).setPastTechnicalDebt(eq(issue), eq(Duration.create(1L)), any(IssueChangeContext.class));
    verify(updater).setPastProject(eq(issue), eq("sample"), any(IssueChangeContext.class));
  }

  @Test
  public void merge_matched_issue_on_manual_severity() throws Exception {
    ServerIssue previousIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setResolution(null).setStatus("OPEN").setRuleKey("squid", "AvoidCycle")
      .setLine(10).setManualSeverity(true).setSeverity("MAJOR").setMessage("Message").setEffortToFix(1.5).setDebt(1L));
    DefaultIssue issue = new DefaultIssue();

    IssueTrackingResult trackingResult = mock(IssueTrackingResult.class);
    when(trackingResult.matched()).thenReturn(newArrayList(issue));
    when(trackingResult.matching(eq(issue))).thenReturn(previousIssue);
    decorator.mergeMatched(trackingResult);

    assertThat(issue.manualSeverity()).isTrue();
    assertThat(issue.severity()).isEqualTo("MAJOR");
    verify(updater, never()).setPastSeverity(eq(issue), anyString(), any(IssueChangeContext.class));
  }

  @Test
  public void merge_issue_changelog_with_previous_changelog() throws Exception {
    when(initialOpenIssues.selectChangelog("ABCDE")).thenReturn(newArrayList(new IssueChangeDto().setIssueKey("ABCD").setCreatedAt(System2.INSTANCE.now())));

    ServerIssue previousIssue = new ServerIssueFromDb(new IssueDto().setKee("ABCDE").setResolution(null).setStatus("OPEN").setRuleKey("squid", "AvoidCycle")
      .setLine(10).setMessage("Message").setEffortToFix(1.5).setDebt(1L).setCreatedAt(System2.INSTANCE.now()));
    DefaultIssue issue = new DefaultIssue();

    IssueTrackingResult trackingResult = mock(IssueTrackingResult.class);
    when(trackingResult.matched()).thenReturn(newArrayList(issue));
    when(trackingResult.matching(eq(issue))).thenReturn(previousIssue);
    decorator.mergeMatched(trackingResult);

    assertThat(issue.changes()).hasSize(1);
  }

  private String[] computeHexHashes(String source) {
    String[] lines = source.split("\n");
    String[] hashes = new String[lines.length];
    for (int i = 0; i < lines.length; i++) {
      hashes[i] = DigestUtils.md5Hex(lines[i].replaceAll("[\t ]", ""));
    }
    return hashes;
  }

}
