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
package org.sonar.server.computation.task.projectanalysis.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.BlockHashSequence;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class ShortBranchTrackerExecutionTest {
  static final String FILE_UUID = "FILE_UUID";
  static final String FILE_KEY = "FILE_KEY";
  static final int FILE_REF = 2;

  static final Component FILE = builder(Component.Type.FILE, FILE_REF)
    .setKey(FILE_KEY)
    .setUuid(FILE_UUID)
    .build();

  @Mock
  private TrackerRawInputFactory rawFactory;
  @Mock
  private TrackerBaseInputFactory baseFactory;
  @Mock
  private TrackerMergeBranchInputFactory mergeFactory;

  private ShortBranchTrackerExecution underTest;

  private List<DefaultIssue> rawIssues = new ArrayList<>();
  private List<DefaultIssue> baseIssues = new ArrayList<>();
  private List<DefaultIssue> mergeBranchIssues = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(rawFactory.create(FILE)).thenReturn(createInput(rawIssues));
    when(baseFactory.create(FILE)).thenReturn(createInput(baseIssues));
    when(mergeFactory.create(FILE)).thenReturn(createInput(mergeBranchIssues));

    Tracker<DefaultIssue, DefaultIssue> tracker = new Tracker<>();
    underTest = new ShortBranchTrackerExecution(baseFactory, rawFactory, mergeFactory, tracker);
  }

  @Test
  public void simple_tracking() {
    rawIssues.add(createIssue(1, RuleTesting.XOO_X1));
    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(FILE);
    assertThat(tracking.getUnmatchedBases()).isEmpty();
    assertThat(tracking.getMatchedRaws()).isEmpty();
    assertThat(tracking.getUnmatchedRaws()).containsOnly(rawIssues.get(0));
  }

  @Test
  public void tracking_with_all_results() {
    rawIssues.add(createIssue(1, RuleTesting.XOO_X1));
    rawIssues.add(createIssue(2, RuleTesting.XOO_X2));
    rawIssues.add(createIssue(3, RuleTesting.XOO_X3));

    mergeBranchIssues.add(rawIssues.get(0));

    baseIssues.add(rawIssues.get(0));
    baseIssues.add(rawIssues.get(1));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(FILE);
    assertThat(tracking.getMatchedRaws()).isEqualTo(Collections.singletonMap(rawIssues.get(1), rawIssues.get(1)));
    assertThat(tracking.getUnmatchedRaws()).containsOnly(rawIssues.get(2));
  }

  private DefaultIssue createIssue(int line, RuleKey ruleKey) {
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setLine(line)
      .setMessage("msg" + line);
    return issue;
  }

  private Input<DefaultIssue> createInput(Collection<DefaultIssue> issues) {
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
