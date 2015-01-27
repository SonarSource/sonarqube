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

import java.text.MessageFormat;

/**
 * @since 4.0
 */
public class DefaultAnalysisMode implements AnalysisMode {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultAnalysisMode.class);

  private boolean preview;
  private boolean incremental;
  private boolean mediumTestMode;

  public DefaultAnalysisMode(BootstrapProperties bootstrapProps) {
    init(bootstrapProps);
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

  private void init(BootstrapProperties bootstrapProps) {
    if (bootstrapProps.properties().containsKey(CoreProperties.DRY_RUN)) {
      LOG.warn(MessageFormat.format("Property {0} is deprecated. Please use {1} instead.", CoreProperties.DRY_RUN, CoreProperties.ANALYSIS_MODE));
      preview = "true".equals(bootstrapProps.property(CoreProperties.DRY_RUN));
      incremental = false;
      mediumTestMode = false;
    } else {
      String mode = bootstrapProps.property(CoreProperties.ANALYSIS_MODE);
      preview = CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
      incremental = CoreProperties.ANALYSIS_MODE_INCREMENTAL.equals(mode);
      mediumTestMode = CoreProperties.ANALYSIS_MODE_MEDIUM_TEST.equals(mode);
    }
    if (incremental) {
      LOG.info("Incremental mode");
    } else if (preview) {
      LOG.info("Preview mode");
    } else if (mediumTestMode) {
      LOG.info("Medium test mode");
    }
    // To stay compatible with plugins that use the old property to check mode
    if (incremental || preview) {
      bootstrapProps.properties().put(CoreProperties.DRY_RUN, "true");
    }
  }

}
