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
package org.sonar.scanner.analysis;

import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.scan.branch.BranchConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultAnalysisModeTest {
  private BranchConfiguration branchConfig;
  private ProjectRepositories projectRepos;
  private GlobalAnalysisMode globalMode;
  private IncrementalScannerHandler incrementalScannerHandler;

  @Before
  public void setUp() {
    branchConfig = mock(BranchConfiguration.class);
    projectRepos = mock(ProjectRepositories.class);
    globalMode = mock(GlobalAnalysisMode.class);
    incrementalScannerHandler = mock(IncrementalScannerHandler.class);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void dont_scan_all_if_short_lived_branch() {
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.scanAllFiles", "true"));
    when(branchConfig.isShortLivingBranch()).thenReturn(true);
    DefaultAnalysisMode mode = createmode(analysisProps);

    assertThat(mode.scanAllFiles()).isFalse();
  }

  @Test
  public void reuse_global_mode() {
    when(globalMode.isIssues()).thenReturn(true);
    when(globalMode.isPublish()).thenReturn(true);
    when(globalMode.isPreview()).thenReturn(true);
    DefaultAnalysisMode mode = createmode(new AnalysisProperties(Collections.emptyMap()));

    assertThat(mode.isIssues()).isTrue();
    assertThat(mode.isPublish()).isTrue();
    assertThat(mode.isPreview()).isTrue();
  }

  @Test
  public void incremental_not_found() {
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.incremental", "true"));
    thrown.expect(MessageException.class);
    thrown.expectMessage("Incremental mode is not available. Please contact your administrator.");
    createmode(analysisProps);
  }

  @Test
  public void no_incremental_if_not_publish() {
    when(incrementalScannerHandler.execute()).thenReturn(true);
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.incremental", "true"));
    thrown.expect(MessageException.class);
    thrown.expectMessage("Incremental analysis is only available in publish mode");
    createmode(analysisProps);
  }

  @Test
  public void no_incremental_mode_if_branches() {
    when(globalMode.isPublish()).thenReturn(true);
    when(incrementalScannerHandler.execute()).thenReturn(true);
    when(branchConfig.branchName()).thenReturn("branch1");
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.incremental", "true"));
    DefaultAnalysisMode analysisMode = createmode(analysisProps);
    assertThat(analysisMode.isIncremental()).isFalse();
    assertThat(analysisMode.scanAllFiles()).isTrue();
  }

  @Test
  public void no_incremental_mode_if_no_previous_analysis() {
    when(incrementalScannerHandler.execute()).thenReturn(true);
    when(globalMode.isPublish()).thenReturn(true);
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.incremental", "true"));
    DefaultAnalysisMode analysisMode = createmode(analysisProps);
    assertThat(analysisMode.isIncremental()).isFalse();
    assertThat(analysisMode.scanAllFiles()).isTrue();
  }

  @Test
  public void incremental_mode() {
    when(incrementalScannerHandler.execute()).thenReturn(true);
    when(globalMode.isPublish()).thenReturn(true);
    when(projectRepos.lastAnalysisDate()).thenReturn(new Date());
    when(projectRepos.exists()).thenReturn(true);
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.incremental", "true"));
    DefaultAnalysisMode analysisMode = createmode(analysisProps);
    assertThat(analysisMode.isIncremental()).isTrue();
    assertThat(analysisMode.scanAllFiles()).isFalse();
  }

  @Test
  public void scan_all_if_publish() {
    when(globalMode.isIssues()).thenReturn(false);
    DefaultAnalysisMode mode = createmode(new AnalysisProperties(Collections.emptyMap()));

    assertThat(mode.scanAllFiles()).isTrue();
  }

  @Test
  public void scan_all_if_property_set() {
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.scanAllFiles", "true"));
    DefaultAnalysisMode mode = createmode(analysisProps);

    assertThat(mode.scanAllFiles()).isTrue();
  }

  @Test
  public void dont_scan_all_if_issues_mode() {
    when(globalMode.isIssues()).thenReturn(true);
    DefaultAnalysisMode mode = createmode(new AnalysisProperties(Collections.emptyMap()));

    assertThat(mode.scanAllFiles()).isFalse();
  }

  private DefaultAnalysisMode createmode(AnalysisProperties analysisProps) {
    return new DefaultAnalysisMode(analysisProps, branchConfig, globalMode, projectRepos, incrementalScannerHandler);
  }

}
