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

import com.google.common.base.Optional;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbCommons.TextRange;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.protobuf.DbIssues.Flow;
import org.sonar.db.protobuf.DbIssues.Location;
import org.sonar.db.protobuf.DbIssues.Locations.Builder;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IssueCreationDateCalculatorTest {
  private static final String COMPONENT_UUID = "ab12";

  @org.junit.Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @org.junit.Rule
  public ExpectedException exception = ExpectedException.none();

  private ScmInfoRepository scmInfoRepository = mock(ScmInfoRepository.class);
  private IssueFieldsSetter issueUpdater = mock(IssueFieldsSetter.class);
  private ActiveRulesHolder activeRulesHolder = mock(ActiveRulesHolder.class);
  private Component component = mock(Component.class);
  private RuleKey ruleKey = RuleKey.of("reop", "rule");
  private DefaultIssue issue = mock(DefaultIssue.class);
  private ActiveRule activeRule = mock(ActiveRule.class);
  private IssueCreationDateCalculator calculator;
  private Analysis baseAnalysis = mock(Analysis.class);
  private Map<String, ScannerPlugin> scannerPlugins = new HashMap<>();
  private RuleRepository ruleRepository = mock(RuleRepository.class);
  private ScmInfo scmInfo;
  private Rule rule = mock(Rule.class);

  @Before
  public void before() {
    analysisMetadataHolder.setScannerPluginsByKey(scannerPlugins);
    analysisMetadataHolder.setAnalysisDate(new Date());
    when(component.getUuid()).thenReturn(COMPONENT_UUID);
    calculator = new IssueCreationDateCalculator(analysisMetadataHolder, scmInfoRepository, issueUpdater, activeRulesHolder, ruleRepository);

    when(ruleRepository.findByKey(ruleKey)).thenReturn(java.util.Optional.of(rule));
    when(activeRulesHolder.get(any(RuleKey.class)))
      .thenReturn(Optional.absent());
    when(activeRulesHolder.get(ruleKey))
      .thenReturn(Optional.of(activeRule));
    when(issue.getRuleKey())
      .thenReturn(ruleKey);
  }

  @Test
  public void should_not_change_date_if_no_scm_available() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    newIssue();
    noScm();
    ruleCreatedAt(2800L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  public void should_not_change_date_if_rule_and_plugin_and_base_plugin_are_old() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    newIssue();
    withScm(1200L);
    ruleCreatedAt(1500L);
    rulePlugin("customjava");
    pluginUpdatedAt("customjava", "java", 1700L);
    pluginUpdatedAt("java", 1700L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  public void should_not_change_date_if_rule_and_plugin_are_old_and_no_base_plugin() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    newIssue();
    withScm(1200L);
    ruleCreatedAt(1500L);
    rulePlugin("java");
    pluginUpdatedAt("java", 1700L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  public void should_not_change_date_if_issue_existed_before() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    existingIssue();
    withScm(1200L);
    ruleCreatedAt(2800L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  public void should_not_fail_for_issue_about_to_be_closed() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    existingIssue();
    when(issue.getRuleKey())
      .thenReturn(RuleKey.of("repo", "disabled"));

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  public void should_fail_if_rule_is_not_found() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    when(ruleRepository.findByKey(ruleKey)).thenReturn(java.util.Optional.empty());
    newIssue();

    exception.expect(IllegalStateException.class);
    exception.expectMessage("The rule with key 'reop:rule' raised an issue, but no rule with that key was found");
    run();
  }

  @Test
  public void should_change_date_if_scm_is_available_and_rule_is_new() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    newIssue();
    withScm(1200L);
    ruleCreatedAt(2800L);

    run();

    assertChangeOfCreationDateTo(1200L);
  }

  @Test
  public void should_change_date_if_scm_is_available_and_first_analysis() {
    analysisMetadataHolder.setBaseAnalysis(null);
    currentAnalysisIs(3000L);

    newIssue();
    withScm(1200L);

    run();

    assertChangeOfCreationDateTo(1200L);
  }

  @Test
  public void should_change_date_if_scm_is_available_and_plugin_is_new() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    newIssue();
    withScm(1200L);
    ruleCreatedAt(1500L);
    rulePlugin("java");
    pluginUpdatedAt("java", 2500L);

    run();

    assertChangeOfCreationDateTo(1200L);
  }

  @Test
  public void should_change_date_if_scm_is_available_and_base_plugin_is_new() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    newIssue();
    withScm(1200L);
    ruleCreatedAt(1500L);
    rulePlugin("customjava");
    pluginUpdatedAt("customjava", "java", 1000L);
    pluginUpdatedAt("java", 2500L);

    run();

    assertChangeOfCreationDateTo(1200L);
  }

  @Test
  public void should_backdate_external_issues() {
    analysisMetadataHolder.setBaseAnalysis(null);
    currentAnalysisIs(3000L);

    newIssue();
    when(rule.isExternal()).thenReturn(true);
    when(issue.getLocations()).thenReturn(DbIssues.Locations.newBuilder().setTextRange(range(2, 3)).build());
    withScmAt(2, 1200L);
    withScmAt(3, 1300L);

    run();

    assertChangeOfCreationDateTo(1300L);
    verifyZeroInteractions(activeRulesHolder);
  }

  @Test
  public void should_use_primary_location_when_backdating() {
    analysisMetadataHolder.setBaseAnalysis(null);
    currentAnalysisIs(3000L);

    newIssue();
    when(issue.getLocations()).thenReturn(DbIssues.Locations.newBuilder().setTextRange(range(2, 3)).build());
    withScmAt(2, 1200L);
    withScmAt(3, 1300L);

    run();

    assertChangeOfCreationDateTo(1300L);
  }

  @Test
  public void should_use_flows_location_when_backdating() {
    analysisMetadataHolder.setBaseAnalysis(null);
    currentAnalysisIs(3000L);

    newIssue();
    Builder builder = DbIssues.Locations.newBuilder()
      .setTextRange(range(2, 3));
    Flow.Builder secondary = Flow.newBuilder().addLocation(Location.newBuilder().setTextRange(range(4, 5)));
    builder.addFlow(secondary).build();
    Flow.Builder flow = Flow.newBuilder()
      .addLocation(Location.newBuilder().setTextRange(range(6, 7)).setComponentId(COMPONENT_UUID))
      .addLocation(Location.newBuilder().setTextRange(range(8, 9)).setComponentId(COMPONENT_UUID));
    builder.addFlow(flow).build();
    when(issue.getLocations()).thenReturn(builder.build());
    withScmAt(2, 1200L);
    withScmAt(3, 1300L);
    withScmAt(4, 1400L);
    withScmAt(5, 1500L);
    // some lines missing should be ok
    withScmAt(9, 1900L);

    run();

    assertChangeOfCreationDateTo(1900L);
  }

  @Test
  public void should_ignore_flows_location_outside_current_file_when_backdating() {
    analysisMetadataHolder.setBaseAnalysis(null);
    currentAnalysisIs(3000L);

    newIssue();
    Builder builder = DbIssues.Locations.newBuilder()
      .setTextRange(range(2, 3));
    Flow.Builder secondary = Flow.newBuilder().addLocation(Location.newBuilder().setTextRange(range(4, 5)));
    builder.addFlow(secondary).build();
    Flow.Builder flow = Flow.newBuilder()
      .addLocation(Location.newBuilder().setTextRange(range(6, 7)).setComponentId(COMPONENT_UUID))
      .addLocation(Location.newBuilder().setTextRange(range(8, 9)).setComponentId("another"));
    builder.addFlow(flow).build();
    when(issue.getLocations()).thenReturn(builder.build());
    withScmAt(2, 1200L);
    withScmAt(3, 1300L);
    withScmAt(4, 1400L);
    withScmAt(5, 1500L);
    withScmAt(6, 1600L);
    withScmAt(7, 1700L);
    withScmAt(8, 1800L);
    withScmAt(9, 1900L);

    run();

    assertChangeOfCreationDateTo(1700L);
  }

  private org.sonar.db.protobuf.DbCommons.TextRange.Builder range(int startLine, int endLine) {
    return TextRange.newBuilder().setStartLine(startLine).setEndLine(endLine);
  }

  private void previousAnalysisWas(long analysisDate) {
    analysisMetadataHolder.setBaseAnalysis(baseAnalysis);
    when(baseAnalysis.getCreatedAt())
      .thenReturn(analysisDate);
  }

  private void pluginUpdatedAt(String pluginKey, long updatedAt) {
    scannerPlugins.put(pluginKey, new ScannerPlugin(pluginKey, null, updatedAt));
  }

  private void pluginUpdatedAt(String pluginKey, String basePluginKey, long updatedAt) {
    scannerPlugins.put(pluginKey, new ScannerPlugin(pluginKey, basePluginKey, updatedAt));
  }

  private void currentAnalysisIs(long analysisDate) {
    analysisMetadataHolder.setAnalysisDate(analysisDate);
  }

  private void newIssue() {
    when(issue.isNew())
      .thenReturn(true);
  }

  private void existingIssue() {
    when(issue.isNew())
      .thenReturn(false);
  }

  private void noScm() {
    when(scmInfoRepository.getScmInfo(component))
      .thenReturn(java.util.Optional.empty());
  }

  private void withScm(long blame) {
    createMockScmInfo();
    Changeset changeset = Changeset.newChangesetBuilder().setDate(blame).setRevision("1").build();
    when(scmInfo.getLatestChangeset()).thenReturn(changeset);
  }

  private void createMockScmInfo() {
    if (scmInfo == null) {
      scmInfo = mock(ScmInfo.class);
      when(scmInfoRepository.getScmInfo(component))
        .thenReturn(java.util.Optional.of(scmInfo));
    }
  }

  private void withScmAt(int line, long blame) {
    createMockScmInfo();
    Changeset changeset = Changeset.newChangesetBuilder().setDate(blame).setRevision("1").build();
    when(scmInfo.hasChangesetForLine(line)).thenReturn(true);
    when(scmInfo.getChangesetForLine(line)).thenReturn(changeset);
  }

  private void ruleCreatedAt(long createdAt) {
    when(activeRule.getCreatedAt()).thenReturn(createdAt);
  }

  private void rulePlugin(String pluginKey) {
    when(activeRule.getPluginKey()).thenReturn(pluginKey);
  }

  private void run() {
    calculator.beforeComponent(component);
    calculator.onIssue(component, issue);
    calculator.afterComponent(component);
  }

  private void assertNoChangeOfCreationDate() {
    verify(issueUpdater, never())
      .setCreationDate(any(), any(), any());
  }

  private void assertChangeOfCreationDateTo(long createdAt) {
    verify(issueUpdater, atLeastOnce())
      .setCreationDate(same(issue), eq(new Date(createdAt)), any());
  }
}
