/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.base.Optional;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.analysis.Analysis;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.server.issue.IssueFieldsSetter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssueCreationDateCalculatorTest {

  private AnalysisMetadataHolder analysisMetadataHolder;
  private ScmInfoRepository scmInfoRepository;
  private IssueFieldsSetter issueUpdater;
  private ActiveRulesHolder activeRulesHolder;
  private Component component;
  private RuleKey ruleKey;
  private DefaultIssue issue;
  private ActiveRule activeRule;
  private IssueCreationDateCalculator calculator;
  private Analysis baseAnalysis;
  private Map<String, ScannerPlugin> scannerPlugins;

  @Before
  public void before() {
    analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
    scannerPlugins = new HashMap<>();
    when(analysisMetadataHolder.getScannerPluginsByKey()).thenReturn(scannerPlugins);
    scmInfoRepository = mock(ScmInfoRepository.class);
    issueUpdater = mock(IssueFieldsSetter.class);
    activeRulesHolder = mock(ActiveRulesHolder.class);
    component = mock(Component.class);
    ruleKey = RuleKey.of("reop", "rule");
    issue = mock(DefaultIssue.class);
    activeRule = mock(ActiveRule.class);
    baseAnalysis = mock(Analysis.class);
    calculator = new IssueCreationDateCalculator(analysisMetadataHolder, scmInfoRepository, issueUpdater, activeRulesHolder);

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
  public void should_not_change_date_if_rule_is_old() {
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

  private void previousAnalysisWas(long analysisDate) {
    when(analysisMetadataHolder.getBaseAnalysis())
      .thenReturn(baseAnalysis);
    when(baseAnalysis.getCreatedAt())
      .thenReturn(analysisDate);
  }

  private void pluginUpdatedAt(String pluginKey, long updatedAt) {
    scannerPlugins.put(pluginKey, new ScannerPlugin(pluginKey, updatedAt));
  }

  private void currentAnalysisIs(long analysisDate) {
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(analysisDate);
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
      .thenReturn(Optional.absent());
  }

  private void withScm(long blame) {
    ScmInfo scmInfo = mock(ScmInfo.class);
    Changeset changeset = Changeset.newChangesetBuilder().setDate(blame).setRevision("1").build();
    when(scmInfoRepository.getScmInfo(component))
      .thenReturn(Optional.of(scmInfo));
    when(scmInfo.getLatestChangeset()).thenReturn(changeset);
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
