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
package org.sonar.batch.bootstrap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAnalysisModeTest {

  @Test
  public void regular_analysis_by_default() {
    DefaultAnalysisMode mode = new DefaultAnalysisMode(Collections.<String, String>emptyMap());

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();

    mode = new DefaultAnalysisMode(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, "pouet"));

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void support_analysis_mode() {
    DefaultAnalysisMode mode = new DefaultAnalysisMode(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ANALYSIS));

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void support_preview_mode() {
    Map<String, String> props = Maps.newHashMap(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW));
    DefaultAnalysisMode mode = new DefaultAnalysisMode(props);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isFalse();

    assertThat(props.get(CoreProperties.DRY_RUN)).isEqualTo("true");
  }

  @Test
  public void support_incremental_mode() {
    Map<String, String> props = Maps.newHashMap(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_INCREMENTAL));
    DefaultAnalysisMode mode = new DefaultAnalysisMode(props);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isTrue();

    assertThat(props.get(CoreProperties.DRY_RUN)).isEqualTo("true");
  }

  @Test
  public void support_deprecated_dryrun_property() {
    DefaultAnalysisMode mode = new DefaultAnalysisMode(Maps.newHashMap(ImmutableMap.of(CoreProperties.DRY_RUN, "true")));

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isFalse();
  }
}
