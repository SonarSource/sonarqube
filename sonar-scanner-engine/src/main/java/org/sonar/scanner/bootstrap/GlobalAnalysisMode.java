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
package org.sonar.scanner.bootstrap;

import java.util.Arrays;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;

@Immutable
public class GlobalAnalysisMode {
  public static final String MEDIUM_TEST_ENABLED = "sonar.mediumTest.enabled";
  private static final Logger LOG = LoggerFactory.getLogger(GlobalAnalysisMode.class);
  private static final String[] VALID_MODES = {CoreProperties.ANALYSIS_MODE_PREVIEW, CoreProperties.ANALYSIS_MODE_PUBLISH, CoreProperties.ANALYSIS_MODE_ISSUES};

  protected boolean preview;
  protected boolean issues;
  protected boolean mediumTestMode;

  public GlobalAnalysisMode(GlobalProperties props) {
    String mode = props.property(CoreProperties.ANALYSIS_MODE);
    validate(mode);
    issues = CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode) || CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
    mediumTestMode = "true".equals(props.property(MEDIUM_TEST_ENABLED));
    if (preview) {
      LOG.info("Preview mode");
      LOG.warn("The use of the preview mode (sonar.analysis.mode=preview) is deprecated. This mode will be dropped in the future.");
    } else if (issues) {
      LOG.info("Issues mode");
      LOG.warn("The use of the issues mode (sonar.analysis.mode=issues) is deprecated. This mode will be dropped in the future.");
    } else {
      LOG.info("Publish mode");
    }
    if (mediumTestMode) {
      LOG.info("Medium test mode");
    }
  }

  public boolean isPreview() {
    return preview;
  }

  public boolean isIssues() {
    return issues;
  }

  public boolean isPublish() {
    return !preview && !issues;
  }

  public boolean isMediumTest() {
    return mediumTestMode;
  }

  protected static void validate(String mode) {
    if (StringUtils.isEmpty(mode)) {
      return;
    }

    if (!Arrays.asList(VALID_MODES).contains(mode)) {
      throw new IllegalStateException("Invalid analysis mode: " + mode + ".");
    }

  }
}
