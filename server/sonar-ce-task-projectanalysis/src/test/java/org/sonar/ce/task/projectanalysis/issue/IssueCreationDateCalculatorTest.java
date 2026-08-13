/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.Date;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
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

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IssueCreationDateCalculatorTest {
  private static final String COMPONENT_UUID = "ab12";

  @RegisterExtension
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private final ScmInfoRepository scmInfoRepository = mock(ScmInfoRepository.class);
  private final IssueFieldsSetter issueUpdater = mock(IssueFieldsSetter.class);
  private final Component component = mock(Component.class);
  private final RuleKey ruleKey = RuleKey.of("reop", "rule");
  private final DefaultIssue issue = mock(DefaultIssue.class);

  private IssueCreationDateCalculator underTest;
  private ScmInfo scmInfo;

  @BeforeEach
  void before() {
    analysisMetadataHolder.setAnalysisDate(new Date(1_704_067_200_000L));
    when(component.getUuid()).thenReturn(COMPONENT_UUID);
    underTest = new IssueCreationDateCalculator(analysisMetadataHolder, scmInfoRepository, issueUpdater);

    when(issue.getRuleKey()).thenReturn(ruleKey);
  }

  @ParameterizedTest
  @MethodSource("backdatingDateCases")
  void should_backdate_to_the_latest_scm_date_of_the_lines_of_the_issue(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    makeIssueNew();
    configure.accept(issue, createMockScmInfo());

    run();

    assertChangeOfCreationDateTo(expectedDate);
  }

  @Test
  void should_not_backdate_if_no_scm_available() {
    makeIssueNew();
    noScm();

    run();

    assertNoChangeOfCreationDate();
  }

  @ParameterizedTest
  @MethodSource("backdatingDateCases")
  void should_not_backdate_if_issue_existed_before(BiConsumer<DefaultIssue, ScmInfo> configure, long expectedDate) {
    makeIssueNotNew();
    configure.accept(issue, createMockScmInfo());

    run();

    assertNoChangeOfCreationDate();
  }

  public static Stream<Arguments> backdatingDateCases() {
    return Stream.of(
      arguments(new NoIssueLocation(), 1200L),
      arguments(new OnlyPrimaryLocation(), 1300L),
      arguments(new FlowOnCurrentFileOnly(), 1900L),
      arguments(new FlowOnMultipleFiles(), 1700L)
    );
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

  private void makeIssueNew() {
    when(issue.isNew())
      .thenReturn(true);
  }

  private void makeIssueNotNew() {
    when(issue.isNew())
      .thenReturn(false);
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
