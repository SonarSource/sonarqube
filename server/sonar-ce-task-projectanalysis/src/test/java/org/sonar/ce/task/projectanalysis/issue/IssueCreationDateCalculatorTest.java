/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.ScannerPlugin;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.filemove.AddedFileRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository;
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
import static org.sonar.ce.task.projectanalysis.qualityprofile.QProfileStatusRepository.Status.UNCHANGED;

@RunWith(DataProviderRunner.class)
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

  private IssueCreationDateCalculator underTest;

  private Analysis baseAnalysis = mock(Analysis.class);
  private Map<String, ScannerPlugin> scannerPlugins = new HashMap<>();
  private RuleRepository ruleRepository = mock(RuleRepository.class);
  private AddedFileRepository addedFileRepository = mock(AddedFileRepository.class);
  private QProfileStatusRepository qProfileStatusRepository = mock(QProfileStatusRepository.class);
  private ScmInfo scmInfo;
  private Rule rule = mock(Rule.class);

  @Before
  public void before() {
    analysisMetadataHolder.setScannerPluginsByKey(scannerPlugins);
    analysisMetadataHolder.setAnalysisDate(new Date());
    when(component.getUuid()).thenReturn(COMPONENT_UUID);
    underTest = new IssueCreationDateCalculator(analysisMetadataHolder, scmInfoRepository, issueUpdater, activeRulesHolder, ruleRepository, addedFileRepository, qProfileStatusRepository);

    when(ruleRepository.findByKey(ruleKey)).thenReturn(Optional.of(rule));
    when(activeRulesHolder.get(any(RuleKey.class))).thenReturn(Optional.empty());
    when(activeRulesHolder.get(ruleKey)).thenReturn(Optional.of(activeRule));
    when(activeRule.getQProfileKey()).thenReturn("qpKey");
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(qProfileStatusRepository.get(any())).thenReturn(Optional.of(UNCHANGED));
  }

  @Test
  public void should_not_backdate_if_no_scm_available() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    noScm();
    setRuleUpdatedAt(2800L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_not_backdate_if_rule_and_plugin_and_base_plugin_are_old(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    setRuleUpdatedAt(1500L);
    rulePlugin("customjava");
    pluginUpdatedAt("customjava", "java", 1700L);
    pluginUpdatedAt("java", 1700L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_not_backdate_if_rule_and_plugin_are_old_and_no_base_plugin(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    setRuleUpdatedAt(1500L);
    rulePlugin("java");
    pluginUpdatedAt("java", 1700L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_not_backdate_if_issue_existed_before(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNotNew();
    configure.accept(issue, createMockScmInfo());
    setRuleUpdatedAt(2800L);

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  public void should_not_fail_for_issue_about_to_be_closed() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNotNew();
    setIssueBelongToNonExistingRule();

    run();

    assertNoChangeOfCreationDate();
  }

  @Test
  public void should_fail_if_rule_is_not_found() {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    when(ruleRepository.findByKey(ruleKey)).thenReturn(Optional.empty());
    makeIssueNew();

    exception.expect(IllegalStateException.class);
    exception.expectMessage("The rule with key 'reop:rule' raised an issue, but no rule with that key was found");
    run();
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_backdate_date_if_scm_is_available_and_rule_is_new(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    setRuleUpdatedAt(2800L);

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_backdate_date_if_scm_is_available_and_rule_has_changed(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    setRuleUpdatedAt(2800L);

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_backdate_date_if_scm_is_available_and_first_analysis(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    currentAnalysisIsFirstAnalysis();
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_backdate_date_if_scm_is_available_and_current_component_is_new_file(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    currentComponentIsNewFile();

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  @UseDataProvider("backdatingDateAndChangedQPStatusCases")
  public void should_backdate_if_qp_of_the_rule_which_raised_the_issue_has_changed(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate, QProfileStatusRepository.Status status) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    changeQualityProfile(status);

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_backdate_if_scm_is_available_and_plugin_is_new(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    setRuleUpdatedAt(1500L);
    rulePlugin("java");
    pluginUpdatedAt("java", 2500L);

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_backdate_if_scm_is_available_and_base_plugin_is_new(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    previousAnalysisWas(2000L);
    currentAnalysisIs(3000L);

    makeIssueNew();
    configure.accept(issue, createMockScmInfo());
    setRuleUpdatedAt(1500L);
    rulePlugin("customjava");
    pluginUpdatedAt("customjava", "java", 1000L);
    pluginUpdatedAt("java", 2500L);

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  @UseDataProvider("backdatingDateCases")
  public void should_backdate_external_issues(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    currentAnalysisIsFirstAnalysis();
    currentAnalysisIs(3000L);

    makeIssueNew();
    when(rule.isExternal()).thenReturn(true);
    configure.accept(issue, createMockScmInfo());

    run();

    assertChangeOfCreationDateTo(expectedDate);
    verifyZeroInteractions(activeRulesHolder);
  }

  @DataProvider
  public static Object[][] backdatingDateAndChangedQPStatusCases() {
    return Stream.of(backdatingDateCases())
      .flatMap(datesCases ->
        Stream.of(QProfileStatusRepository.Status.values())
          .filter(s -> !UNCHANGED.equals(s))
          .map(s -> ArrayUtils.add(datesCases, s)))
      .toArray(Object[][]::new);
  }

  @DataProvider
  public static Object[][] backdatingDateCases() {
    return new Object[][] {
      {new NoIssueLocation(), 1200L},
      {new OnlyPrimaryLocation(), 1300L},
      {new FlowOnCurrentFileOnly(), 1900L},
      {new FlowOnMultipleFiles(), 1700L}
    };
  }

  private static class NoIssueLocation implements BiConsumer<DefaultIssue, ScmInfo> {
    @Override
    public void accept(DefaultIssue issue, ScmInfo scmInfo) {
      setDateOfLatestScmChangeset(scmInfo, 1200L);
    }
  }

  private static class OnlyPrimaryLocation implements BiConsumer<DefaultIssue, ScmInfo> {
    @Override
    public void accept(DefaultIssue issue, ScmInfo scmInfo) {
      when(issue.getLocations()).thenReturn(DbIssues.Locations.newBuilder().setTextRange(range(2, 3)).build());
      setDateOfChangetsetAtLine(scmInfo, 2, 1200L);
      setDateOfChangetsetAtLine(scmInfo, 3, 1300L);
    }
  }

  private static class FlowOnCurrentFileOnly implements BiConsumer<DefaultIssue, ScmInfo> {
    @Override
    public void accept(DefaultIssue issue, ScmInfo scmInfo) {
      Builder locations = DbIssues.Locations.newBuilder()
        .setTextRange(range(2, 3))
        .addFlow(newFlow(newLocation(4, 5)))
        .addFlow(newFlow(newLocation(6, 7, COMPONENT_UUID), newLocation(8, 9, COMPONENT_UUID)));
      when(issue.getLocations()).thenReturn(locations.build());
      setDateOfChangetsetAtLine(scmInfo, 2, 1200L);
      setDateOfChangetsetAtLine(scmInfo, 3, 1300L);
      setDateOfChangetsetAtLine(scmInfo, 4, 1400L);
      setDateOfChangetsetAtLine(scmInfo, 5, 1500L);
      // some lines missing should be ok
      setDateOfChangetsetAtLine(scmInfo, 9, 1900L);
    }
  }

  private static class FlowOnMultipleFiles implements BiConsumer<DefaultIssue, ScmInfo> {
    @Override
    public void accept(DefaultIssue issue, ScmInfo scmInfo) {
      Builder locations = DbIssues.Locations.newBuilder()
        .setTextRange(range(2, 3))
        .addFlow(newFlow(newLocation(4, 5)))
        .addFlow(newFlow(newLocation(6, 7, COMPONENT_UUID), newLocation(8, 9, "another")));
      when(issue.getLocations()).thenReturn(locations.build());
      setDateOfChangetsetAtLine(scmInfo, 2, 1200L);
      setDateOfChangetsetAtLine(scmInfo, 3, 1300L);
      setDateOfChangetsetAtLine(scmInfo, 4, 1400L);
      setDateOfChangetsetAtLine(scmInfo, 5, 1500L);
      setDateOfChangetsetAtLine(scmInfo, 6, 1600L);
      setDateOfChangetsetAtLine(scmInfo, 7, 1700L);
      setDateOfChangetsetAtLine(scmInfo, 8, 1800L);
      setDateOfChangetsetAtLine(scmInfo, 9, 1900L);
    }
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

  private AnalysisMetadataHolderRule currentAnalysisIsFirstAnalysis() {
    return analysisMetadataHolder.setBaseAnalysis(null);
  }

  private void currentAnalysisIs(long analysisDate) {
    analysisMetadataHolder.setAnalysisDate(analysisDate);
  }

  private void currentComponentIsNewFile() {
    when(component.getType()).thenReturn(Component.Type.FILE);
    when(addedFileRepository.isAdded(component)).thenReturn(true);
  }

  private void makeIssueNew() {
    when(issue.isNew())
      .thenReturn(true);
  }

  private void makeIssueNotNew() {
    when(issue.isNew())
      .thenReturn(false);
  }

  private void changeQualityProfile(QProfileStatusRepository.Status status) {
    when(qProfileStatusRepository.get(any())).thenReturn(Optional.of(status));
  }

  private void setIssueBelongToNonExistingRule() {
    when(issue.getRuleKey())
      .thenReturn(RuleKey.of("repo", "disabled"));
  }

  private void noScm() {
    when(scmInfoRepository.getScmInfo(component))
      .thenReturn(Optional.empty());
  }

  private static void setDateOfLatestScmChangeset(ScmInfo scmInfo, long date) {
    Changeset changeset = Changeset.newChangesetBuilder().setDate(date).setRevision("1").build();
    when(scmInfo.getLatestChangeset()).thenReturn(changeset);
  }

  private static void setDateOfChangetsetAtLine(ScmInfo scmInfo, int line, long date) {
    Changeset changeset = Changeset.newChangesetBuilder().setDate(date).setRevision("1").build();
    when(scmInfo.hasChangesetForLine(line)).thenReturn(true);
    when(scmInfo.getChangesetForLine(line)).thenReturn(changeset);
  }

  private ScmInfo createMockScmInfo() {
    if (scmInfo == null) {
      scmInfo = mock(ScmInfo.class);
      when(scmInfoRepository.getScmInfo(component))
        .thenReturn(Optional.of(scmInfo));
    }
    return scmInfo;
  }

  private void setRuleUpdatedAt(long updateAt) {
    when(activeRule.getUpdatedAt()).thenReturn(updateAt);
  }

  private void rulePlugin(String pluginKey) {
    when(activeRule.getPluginKey()).thenReturn(pluginKey);
  }

  private static Location newLocation(int startLine, int endLine) {
    return Location.newBuilder().setTextRange(range(startLine, endLine)).build();
  }

  private static Location newLocation(int startLine, int endLine, String componentUuid) {
    return Location.newBuilder().setTextRange(range(startLine, endLine)).setComponentId(componentUuid).build();
  }

  private static org.sonar.db.protobuf.DbCommons.TextRange range(int startLine, int endLine) {
    return TextRange.newBuilder().setStartLine(startLine).setEndLine(endLine).build();
  }

  private static Flow newFlow(Location... locations) {
    Flow.Builder builder = Flow.newBuilder();
    Arrays.stream(locations).forEach(builder::addLocation);
    return builder.build();
  }

  private void run() {
    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);
    underTest.afterComponent(component);
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
