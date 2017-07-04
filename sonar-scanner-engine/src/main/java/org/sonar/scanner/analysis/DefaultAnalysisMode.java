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

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.scanner.bootstrap.AbstractAnalysisMode;
import org.sonar.scanner.bootstrap.GlobalProperties;

/**
 * @since 4.0
 */
@Immutable
public class DefaultAnalysisMode extends AbstractAnalysisMode {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAnalysisMode.class);
  private static final String KEY_SCAN_ALL = "sonar.scanAllFiles";

  private boolean scanAllFiles;

  public DefaultAnalysisMode(GlobalProperties globalProps, AnalysisProperties props) {
    init(globalProps.properties(), props.properties());
  }

  public boolean scanAllFiles() {
    return scanAllFiles;
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
    issues = CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode) || CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
    mediumTestMode = "true".equals(getPropertyWithFallback(analysisProps, globalProps, MEDIUM_TEST_ENABLED));
    String scanAllStr = getPropertyWithFallback(analysisProps, globalProps, KEY_SCAN_ALL);
    scanAllFiles = !issues || "true".equals(scanAllStr);
  }

  public void printMode() {
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
    if (!scanAllFiles) {
      LOG.info("Scanning only changed files");
    }
  }

  @CheckForNull
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
}
