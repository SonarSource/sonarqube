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
package org.sonar.scanner.analysis;

import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultAnalysisModeTest {
  private GlobalAnalysisMode globalMode;

  @Before
  public void setUp() {
    globalMode = mock(GlobalAnalysisMode.class);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void scan_all_even_on_short_lived_branch() {
    AnalysisProperties analysisProps = new AnalysisProperties(Collections.singletonMap("sonar.scanAllFiles", "true"));
    DefaultAnalysisMode mode = createmode(analysisProps);

    assertThat(mode.scanAllFiles()).isTrue();
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
    return new DefaultAnalysisMode(analysisProps, globalMode);
  }

}
