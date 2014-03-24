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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class AnalysisModeTest {

  BootstrapSettings bootstrapSettings;

  @Before
  public void prepare() {
    bootstrapSettings = new BootstrapSettings(new BootstrapProperties(Collections.<String, String>emptyMap()));
  }

  @Test
  public void regular_analysis_by_default() {
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();

    bootstrapSettings.properties().put(CoreProperties.ANALYSIS_MODE, "pouet");
    mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void support_analysis_mode() {
    bootstrapSettings.properties().put(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ANALYSIS);
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void support_preview_mode() {
    bootstrapSettings.properties().put(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW);
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isFalse();

    assertThat(bootstrapSettings.property(CoreProperties.DRY_RUN)).isEqualTo("true");
  }

  @Test
  public void support_incremental_mode() {
    bootstrapSettings.properties().put(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_INCREMENTAL);
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isTrue();

    assertThat(bootstrapSettings.property(CoreProperties.DRY_RUN)).isEqualTo("true");
  }

  @Test
  public void support_deprecated_dryrun_property() {
    bootstrapSettings.properties().put(CoreProperties.DRY_RUN, "true");
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void should_get_default_preview_read_timeout() {
    bootstrapSettings.properties().put(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW);
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.getPreviewReadTimeoutSec()).isEqualTo(60);
  }

  @Test
  public void should_download_database_with_deprecated_overriden_timeout() {
    bootstrapSettings.properties().put(CoreProperties.DRY_RUN, "true");
    bootstrapSettings.properties().put(CoreProperties.DRY_RUN_READ_TIMEOUT_SEC, "80");
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.getPreviewReadTimeoutSec()).isEqualTo(80);
  }

  @Test
  public void should_download_database_with_overriden_timeout() {
    bootstrapSettings.properties().put(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW);
    bootstrapSettings.properties().put(CoreProperties.PREVIEW_READ_TIMEOUT_SEC, "80");
    AnalysisMode mode = new AnalysisMode(bootstrapSettings);

    assertThat(mode.getPreviewReadTimeoutSec()).isEqualTo(80);
  }
}
