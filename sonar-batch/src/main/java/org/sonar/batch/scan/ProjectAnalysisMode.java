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
package org.sonar.batch.scan;

import org.sonar.batch.bootstrap.BootstrapProperties;

import org.sonar.batch.bootstrap.AnalysisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.mediumtest.BatchMediumTester;

import java.text.MessageFormat;
import java.util.Map;

/**
 * @since 4.0
 */
public class ProjectAnalysisMode implements AnalysisMode {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectAnalysisMode.class);

  private boolean preview;
  private boolean quick;
  private boolean mediumTestMode;

  public ProjectAnalysisMode(BootstrapProperties globalProps, AnalysisProperties props) {
    init(globalProps.properties(), props.properties());
  }

  @Override
  public boolean isPreview() {
    return preview || quick;
  }

  @Override
  public boolean isQuick() {
    return quick;
  }

  public boolean isMediumTest() {
    return mediumTestMode;
  }

  private void init(Map<String, String> globalProps, Map<String, String> analysisProps) {
    // make sure analysis is consistent with global properties
    boolean globalPreview = isPreview(globalProps);
    boolean analysisPreview = isPreview(analysisProps);

    if (!globalPreview && analysisPreview) {
      throw new IllegalStateException("Inconsistent properties:  global properties doesn't enable preview mode while analysis properties enables it");
    }

    load(globalProps, analysisProps);
  }

  private void load(Map<String, String> globalProps, Map<String, String> analysisProps) {
    if (getPropertyWithFallback(analysisProps, globalProps, CoreProperties.DRY_RUN) != null) {
      LOG.warn(MessageFormat.format("Property {0} is deprecated. Please use {1} instead.", CoreProperties.DRY_RUN, CoreProperties.ANALYSIS_MODE));
      preview = "true".equals(getPropertyWithFallback(analysisProps, globalProps, CoreProperties.DRY_RUN));
    } else {
      String mode = getPropertyWithFallback(analysisProps, globalProps, CoreProperties.ANALYSIS_MODE);
      preview = CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
      quick = CoreProperties.ANALYSIS_MODE_QUICK.equals(mode);
    }
    mediumTestMode = "true".equals(getPropertyWithFallback(analysisProps, globalProps, BatchMediumTester.MEDIUM_TEST_ENABLED));

    if (preview) {
      LOG.info("Preview mode");
    } else if (quick) {
      LOG.info("Quick mode");
    }
    if (mediumTestMode) {
      LOG.info("Medium test mode");
    }
  }

  private static String getPropertyWithFallback(Map<String, String> props1, Map<String, String> props2, String key) {
    if (props1.containsKey(key)) {
      return props1.get(key);
    }

    return props2.get(key);
  }

  private static boolean isPreview(Map<String, String> props) {
    String mode = props.get(CoreProperties.ANALYSIS_MODE);

    return "true".equals(props.get(CoreProperties.DRY_RUN)) || CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode) ||
      CoreProperties.ANALYSIS_MODE_QUICK.equals(mode);
  }
}
