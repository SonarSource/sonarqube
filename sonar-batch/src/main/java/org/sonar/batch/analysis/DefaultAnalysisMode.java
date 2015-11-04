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
package org.sonar.batch.analysis;

import java.util.Map;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.bootstrap.AbstractAnalysisMode;
import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.batch.mediumtest.FakePluginInstaller;

/**
 * @since 4.0
 */
public class DefaultAnalysisMode extends AbstractAnalysisMode implements AnalysisMode {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAnalysisMode.class);
  private static final String KEY_SCAN_ALL = "sonar.scanAllFiles";

  private boolean mediumTestMode;
  private boolean notAssociated;
  private boolean scanAllFiles;

  public DefaultAnalysisMode(GlobalProperties globalProps, AnalysisProperties props) {
    init(globalProps.properties(), props.properties());
  }

  public boolean isMediumTest() {
    return mediumTestMode;
  }

  public boolean isNotAssociated() {
    return notAssociated;
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
    mediumTestMode = "true".equals(getPropertyWithFallback(analysisProps, globalProps, FakePluginInstaller.MEDIUM_TEST_ENABLED));
    notAssociated = issues && rootProjectKeyMissing(analysisProps);
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
    if (notAssociated) {
      LOG.info("Local analysis");
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

  private static boolean rootProjectKeyMissing(Map<String, String> props) {
    // ProjectReactorBuilder depends on this class, so it will only create this property later
    return !props.containsKey(CoreProperties.PROJECT_KEY_PROPERTY);
  }

}
