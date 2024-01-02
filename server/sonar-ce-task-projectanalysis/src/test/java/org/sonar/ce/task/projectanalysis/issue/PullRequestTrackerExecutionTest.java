/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.BlockHashSequence;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class PullRequestTrackerExecutionTest {
  private static final String FILE_UUID = "FILE_UUID";
  private static final String FILE_KEY = "FILE_KEY";
  private static final int FILE_REF = 2;

  private static final Component FILE = builder(Component.Type.FILE, FILE_REF)
    .setKey(FILE_KEY)
    .setUuid(FILE_UUID)
    .build();

  private final TrackerBaseInputFactory baseFactory = mock(TrackerBaseInputFactory.class);
  private final NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);
  private final List<DefaultIssue> rawIssues = new ArrayList<>();
  private final List<DefaultIssue> baseIssues = new ArrayList<>();
  private final List<DefaultIssue> targetIssues = new ArrayList<>();

  private PullRequestTrackerExecution underTest;

  private TrackerTargetBranchInputFactory targetFactory = mock(TrackerTargetBranchInputFactory.class);

  @Before
  public void setUp() {
    when(baseFactory.create(FILE)).thenReturn(createInput(baseIssues));
    when(targetFactory.createForTargetBranch(FILE)).thenReturn(createInput(targetIssues));

    Tracker<DefaultIssue, DefaultIssue> tracker = new Tracker<>();
    underTest = new PullRequestTrackerExecution(baseFactory, targetFactory, tracker, newLinesRepository);
  }

  @Test
  public void simple_tracking_keep_only_issues_having_location_on_changed_lines() {
    final DefaultIssue issue1 = createIssue(2, 3, RuleTesting.XOO_X1);
    rawIssues.add(issue1);
    rawIssues.add(createIssue(2, RuleTesting.XOO_X1));

    when(newLinesRepository.getNewLines(FILE)).thenReturn(Optional.of(new HashSet<>(Arrays.asList(1, 3))));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(FILE, createInput(rawIssues));

    assertThat(tracking.getUnmatchedBases()).isEmpty();
    assertThat(tracking.getMatchedRaws()).isEmpty();
    assertThat(tracking.getUnmatchedRaws()).containsOnly(issue1);
  }

  @Test
  public void simple_tracking_keep_also_issues_having_secondary_locations_on_changed_lines() {
    final DefaultIssue issueWithSecondaryLocationOnAChangedLine = createIssue(2, RuleTesting.XOO_X1);
    issueWithSecondaryLocationOnAChangedLine.setLocations(DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(2).setEndLine(3))
      .addFlow(DbIssues.Flow.newBuilder().addLocation(DbIssues.Location.newBuilder()
        .setComponentId(FILE.getUuid())
        .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(6).setEndLine(8)).build()).build())
      .build());
    rawIssues.add(issueWithSecondaryLocationOnAChangedLine);
    final DefaultIssue issueWithNoLocationsOnChangedLines = createIssue(2, RuleTesting.XOO_X1);
    issueWithNoLocationsOnChangedLines.setLocations(DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(2).setEndLine(2))
      .addFlow(DbIssues.Flow.newBuilder().addLocation(DbIssues.Location.newBuilder()
        .setComponentId(FILE.getUuid())
        .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(11).setEndLine(12)).build()).build())
      .build());
    rawIssues.add(issueWithNoLocationsOnChangedLines);
    final DefaultIssue issueWithALocationOnADifferentFile = createIssue(2, RuleTesting.XOO_X1);
    issueWithALocationOnADifferentFile.setLocations(DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(2).setEndLine(3))
      .addFlow(DbIssues.Flow.newBuilder().addLocation(DbIssues.Location.newBuilder()
        .setComponentId("anotherUuid")
        .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(6).setEndLine(8)).build()).build())
      .build());
    rawIssues.add(issueWithALocationOnADifferentFile);

    when(newLinesRepository.getNewLines(FILE)).thenReturn(Optional.of(new HashSet<>(Arrays.asList(7, 10))));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(FILE, createInput(rawIssues));

    assertThat(tracking.getUnmatchedBases()).isEmpty();
    assertThat(tracking.getMatchedRaws()).isEmpty();
    assertThat(tracking.getUnmatchedRaws()).containsOnly(issueWithSecondaryLocationOnAChangedLine);
  }

  @Test
  public void track_and_ignore_resolved_issues_from_target_branch() {
    when(newLinesRepository.getNewLines(FILE)).thenReturn(Optional.of(new HashSet<>(Arrays.asList(1, 2, 3))));

    rawIssues.add(createIssue(1, RuleTesting.XOO_X1));
    rawIssues.add(createIssue(2, RuleTesting.XOO_X2));
    rawIssues.add(createIssue(3, RuleTesting.XOO_X3));

    when(targetFactory.hasTargetBranchAnalysis()).thenReturn(true);
    DefaultIssue resolvedIssue = createIssue(1, RuleTesting.XOO_X1).setStatus(Issue.STATUS_RESOLVED).setResolution(Issue.RESOLUTION_FALSE_POSITIVE);
    // will cause rawIssue0 to be ignored
    targetIssues.add(resolvedIssue);
    // not ignored since it's not resolved
    targetIssues.add(rawIssues.get(1));

    baseIssues.add(rawIssues.get(0));
    // should be matched
    baseIssues.add(rawIssues.get(1));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(FILE, createInput(rawIssues));
    assertThat(tracking.getMatchedRaws()).isEqualTo(Collections.singletonMap(rawIssues.get(1), rawIssues.get(1)));
    assertThat(tracking.getUnmatchedRaws()).containsOnly(rawIssues.get(2));
  }

  @Test
  public void track_and_ignore_issues_from_previous_analysis() {
    when(newLinesRepository.getNewLines(FILE)).thenReturn(Optional.of(new HashSet<>(Arrays.asList(1, 2, 3))));

    rawIssues.add(createIssue(1, RuleTesting.XOO_X1));
    rawIssues.add(createIssue(2, RuleTesting.XOO_X2));
    rawIssues.add(createIssue(3, RuleTesting.XOO_X3));
    baseIssues.add(rawIssues.get(0));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(FILE, createInput(rawIssues));
    assertThat(tracking.getMatchedRaws()).isEqualTo(Collections.singletonMap(rawIssues.get(0), rawIssues.get(0)));
    assertThat(tracking.getUnmatchedRaws()).containsOnly(rawIssues.get(2));
  }

  private DefaultIssue createIssue(int line, RuleKey ruleKey) {
    return createIssue(line, line, ruleKey);
  }

  private DefaultIssue createIssue(int startLine, int endLine, RuleKey ruleKey) {
    return new DefaultIssue()
      .setRuleKey(ruleKey)
      .setLine(startLine)
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(DbCommons.TextRange.newBuilder().setStartLine(startLine).setEndLine(endLine)).build())
      .setMessage("msg" + startLine);
  }

  private static Input<DefaultIssue> createInput(Collection<DefaultIssue> issues) {
    return new Input<DefaultIssue>() {
      @Override
      public LineHashSequence getLineHashSequence() {
        return LineHashSequence.createForLines(Arrays.asList("line1", "line2", "line3"));
      }

      @Override
      public BlockHashSequence getBlockHashSequence() {
        return BlockHashSequence.create(getLineHashSequence());
      }

      @Override
      public Collection<DefaultIssue> getIssues() {
        return issues;
      }
    };
  }
}
