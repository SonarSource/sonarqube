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
package org.sonar.ce.task.projectanalysis.issue.introducedissues;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonarsource.history.model.IntroducedIssueForHistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntroducedIssueVisitorTest {
  private static final long ANALYSIS_DATE = 1_794_873_600_123L;

  private final AnalysisMetadataHolder analysisMetadataHolder = mock();
  private final TreeRootHolder treeRootHolder = mock();
  private final Branch branch = mock();
  private final Component root = mock();
  private final IntroducedIssueVisitor underTest = new IntroducedIssueVisitor(analysisMetadataHolder, treeRootHolder);

  @BeforeEach
  void setUp() {
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(ANALYSIS_DATE);
    when(treeRootHolder.getRoot()).thenReturn(root);
    when(root.getUuid()).thenReturn("main-branch");
    when(branch.isMain()).thenReturn(true);
  }

  @ParameterizedTest
  @EnumSource(value = RuleType.class, names = "SECURITY_HOTSPOT", mode = EnumSource.Mode.EXCLUDE)
  void onIssue_whenNewIssueOnMainBranch_collectsIntroductionOnAnalysisDate(RuleType type) {
    underTest.onIssue(root, newIssue().setType(type));

    assertThat(underTest.getIntroducedIssues()).containsExactly(new IntroducedIssueForHistory("issue", "main-branch", ANALYSIS_DATE,
      type.name(), "MAJOR", Map.of(SoftwareQuality.RELIABILITY.name(), Severity.HIGH.name())));
  }

  @Test
  void onIssue_whenNotMainBranch_ignoresIssue() {
    when(branch.isMain()).thenReturn(false);

    underTest.onIssue(root, newIssue());

    assertThat(underTest.getIntroducedIssues()).isEmpty();
  }

  @Test
  void onIssue_whenIssueIsNotNew_ignoresIssue() {
    underTest.onIssue(root, newIssue().setNew(false));

    assertThat(underTest.getIntroducedIssues()).isEmpty();
  }

  @Test
  void onIssue_whenSecurityHotspot_ignoresIssue() {
    underTest.onIssue(root, newIssue().setType(RuleType.SECURITY_HOTSPOT));

    assertThat(underTest.getIntroducedIssues()).isEmpty();
  }

  @Test
  void onIssue_whenTypeOrSeverityIsMissing_ignoresIssue() {
    underTest.onIssue(root, newIssue().setType(null));
    underTest.onIssue(root, newIssue().setSeverity(null));

    assertThat(underTest.getIntroducedIssues()).isEmpty();
  }

  private static DefaultIssue newIssue() {
    return new DefaultIssue().setKey("issue").setNew(true).setType(RuleType.BUG).setSeverity("MAJOR")
      .addImpact(SoftwareQuality.RELIABILITY, Severity.HIGH);
  }
}
