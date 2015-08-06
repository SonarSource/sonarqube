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

import org.apache.commons.lang.StringUtils;
import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.bootstrap.AnalysisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.mediumtest.BatchMediumTester;

import java.util.Map;

/**
 * @since 4.0
 */
public class ProjectAnalysisMode implements AnalysisMode {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectAnalysisMode.class);

  private boolean preview;
  private boolean issues;
  private boolean mediumTestMode;

  public ProjectAnalysisMode(BootstrapProperties globalProps, AnalysisProperties props) {
    init(globalProps.properties(), props.properties());
  }

  @Override
  public boolean isPreview() {
    return preview;
  }

  @Override
  public boolean isIssues() {
    return issues;
  }

  @Override
  public boolean isPublish() {
    return !preview && !issues;
  }

  public boolean isMediumTest() {
    return mediumTestMode;
  }

  private void init(Map<String, String> globalProps, Map<String, String> analysisProps) {
    // make sure analysis is consistent with global properties
    boolean globalPreview = isIssues(globalProps);
    boolean analysisPreview = isIssues(analysisProps);

    if (!globalPreview && analysisPreview) {
      throw new IllegalStateException("Inconsistent properties:  global properties doesn't enable issues mode while analysis properties enables it");
    }

    load(globalProps, analysisProps);
  }

  private void load(Map<String, String> globalProps, Map<String, String> analysisProps) {
    String mode = getPropertyWithFallback(analysisProps, globalProps, CoreProperties.ANALYSIS_MODE);
    validate(mode);
    preview = CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
    issues = CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode);
    mediumTestMode = "true".equals(getPropertyWithFallback(analysisProps, globalProps, BatchMediumTester.MEDIUM_TEST_ENABLED));

    if (preview) {
      LOG.info("Preview mode");
    } else if (issues) {
      LOG.info("Issues mode");
    } else {
      LOG.info("Publish mode");
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

  private static boolean isIssues(Map<String, String> props) {
    String mode = props.get(CoreProperties.ANALYSIS_MODE);

    return CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode);
  }

  private static void validate(String mode) {
    if (StringUtils.isEmpty(mode)) {
      return;
    }

    if (!CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode) && !CoreProperties.ANALYSIS_MODE_PUBLISH.equals(mode) &&
      !CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode)) {
      throw new IllegalStateException("Invalid analysis mode: " + mode);
    }
  }
}
