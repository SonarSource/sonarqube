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
public class DefaultAnalysisMode implements AnalysisMode {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAnalysisMode.class);

  private boolean preview;
  private boolean incremental;
  private boolean mediumTestMode;

  public DefaultAnalysisMode(Map<String, String> props) {
    init(props);
  }

  public boolean isDb() {
    return !preview && !incremental && !mediumTestMode;
  }

  @Override
  public boolean isPreview() {
    return preview || incremental;
  }

  @Override
  public boolean isIncremental() {
    return incremental;
  }

  public boolean isMediumTest() {
    return mediumTestMode;
  }

  private void init(Map<String, String> props) {
    if (props.containsKey(CoreProperties.DRY_RUN)) {
      LOG.warn(MessageFormat.format("Property {0} is deprecated. Please use {1} instead.", CoreProperties.DRY_RUN, CoreProperties.ANALYSIS_MODE));
      preview = "true".equals(props.get(CoreProperties.DRY_RUN));
      incremental = false;
    } else {
      String mode = props.get(CoreProperties.ANALYSIS_MODE);
      preview = CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
      incremental = CoreProperties.ANALYSIS_MODE_INCREMENTAL.equals(mode);
    }
    mediumTestMode = "true".equals(props.get(BatchMediumTester.MEDIUM_TEST_ENABLED));
    if (incremental) {
      LOG.info("Incremental mode");
    } else if (preview) {
      LOG.info("Preview mode");
    }
    if (mediumTestMode) {
      LOG.info("Medium test mode");
    }
    // To stay compatible with plugins that use the old property to check mode
    if (incremental || preview) {
      props.put(CoreProperties.DRY_RUN, "true");
    }
  }

}
