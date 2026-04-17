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
package org.sonar.scanner.report;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.issue.IssueResolution;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueResolution;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.issue.IssueResolutionCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class IssueResolutionPublisherTest {
  private final ScannerReportWriter writer = mock(ScannerReportWriter.class);
  private final IssueResolutionCache cache = new IssueResolutionCache();
  private final IssueResolutionPublisher underTest = new IssueResolutionPublisher(cache);

  @Test
  void publish_does_nothing_when_cache_is_empty() {
    underTest.publish(writer);

    verifyNoInteractions(writer);
  }

  @Test
  void publish_writes_issue_resolution_to_report() {
    DefaultInputFile file1 = new TestInputFileBuilder("foo", "src/Foo.java")
      .setLines(30)
      .setOriginalLineStartOffsets(new int[30])
      .setOriginalLineEndOffsets(new int[] {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300})
      .setLastValidOffset(300)
      .build();
    DefaultInputFile file2 = new TestInputFileBuilder("foo", "src/Bar.java")
      .setLines(30)
      .setOriginalLineStartOffsets(new int[30])
      .setOriginalLineEndOffsets(new int[] {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300})
      .setLastValidOffset(300)
      .build();

    DefaultIssueResolution resolution1 = new DefaultIssueResolution(null);
    resolution1.forRules(java.util.Set.of(RuleKey.of("java", "S1234")))
      .on(file1)
      .at(file1.newRange(10, 0, 10, 5))
      .comment("accepted")
      .status(IssueResolution.Status.DEFAULT);
    cache.add(resolution1);

    DefaultIssueResolution resolution2 = new DefaultIssueResolution(null);
    resolution2.forRules(java.util.Set.of(RuleKey.of("java", "S5678")))
      .on(file2)
      .at(file2.newRange(20, 0, 21, 5))
      .comment("false positive")
      .status(IssueResolution.Status.FALSE_POSITIVE);
    cache.add(resolution2);

    underTest.publish(writer);

    verify(writer).writeIssueResolution(eq(file1.scannerId()), argThat(data -> {
      List<ScannerReport.IssueResolution> list = new java.util.ArrayList<>();
      data.forEach(list::add);
      return list.size() == 1
        && list.get(0).getRuleKeysList().contains("java:S1234")
        && list.get(0).hasTextRange()
        && list.get(0).getTextRange().getStartLine() == 10
        && list.get(0).getComment().equals("accepted")
        && list.get(0).getStatus() == ScannerReport.IssueResolutionStatus.DEFAULT;
    }));
    verify(writer).writeIssueResolution(eq(file2.scannerId()), argThat(data -> {
      List<ScannerReport.IssueResolution> list = new java.util.ArrayList<>();
      data.forEach(list::add);
      return list.size() == 1
        && list.get(0).getRuleKeysList().contains("java:S5678")
        && list.get(0).hasTextRange()
        && list.get(0).getTextRange().getStartLine() == 20
        && list.get(0).getTextRange().getEndLine() == 21
        && list.get(0).getComment().equals("false positive")
        && list.get(0).getStatus() == ScannerReport.IssueResolutionStatus.FALSE_POSITIVE;
    }));
  }

  @Test
  void publish_writes_multiple_issue_resolutions_for_same_component() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.java")
      .setLines(30)
      .setOriginalLineStartOffsets(new int[30])
      .setOriginalLineEndOffsets(new int[] {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300})
      .setLastValidOffset(300)
      .build();

    DefaultIssueResolution resolution1 = new DefaultIssueResolution(null);
    resolution1.forRules(java.util.Set.of(RuleKey.of("java", "S1234")))
      .on(file)
      .at(file.newRange(10, 0, 10, 5))
      .comment("first")
      .status(IssueResolution.Status.DEFAULT);
    cache.add(resolution1);

    DefaultIssueResolution resolution2 = new DefaultIssueResolution(null);
    resolution2.forRules(java.util.Set.of(RuleKey.of("java", "S5678")))
      .on(file)
      .at(file.newRange(20, 0, 20, 5))
      .comment("second")
      .status(IssueResolution.Status.FALSE_POSITIVE);
    cache.add(resolution2);

    underTest.publish(writer);

    verify(writer).writeIssueResolution(eq(file.scannerId()), argThat(data -> {
      List<ScannerReport.IssueResolution> list = new java.util.ArrayList<>();
      data.forEach(list::add);
      return list.size() == 2;
    }));
  }
}
