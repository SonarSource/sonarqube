/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.postjob;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.api.config.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostJobOptimizerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PostJobOptimizer optimizer;
  private Settings settings;
  private AnalysisMode analysisMode;

  @Before
  public void prepare() {
    settings = new Settings();
    analysisMode = mock(AnalysisMode.class);
    optimizer = new PostJobOptimizer(settings, analysisMode);
  }

  @Test
  public void should_run_analyzer_with_no_metadata() {
    DefaultPostJobDescriptor descriptor = new DefaultPostJobDescriptor();

    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_settings() {
    DefaultPostJobDescriptor descriptor = new DefaultPostJobDescriptor()
      .requireProperty("sonar.foo.reportPath");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    settings.setProperty("sonar.foo.reportPath", "foo");
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_disabled_in_issues_mode() {
    DefaultPostJobDescriptor descriptor = new DefaultPostJobDescriptor()
      .disabledInIssues();
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();

    when(analysisMode.isIssues()).thenReturn(true);

    assertThat(optimizer.shouldExecute(descriptor)).isFalse();
  }
}
