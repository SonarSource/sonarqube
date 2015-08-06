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

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;

public class GlobalMode {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalMode.class);
  private boolean preview;
  private boolean issues;

  public boolean isPreview() {
    return preview;
  }
  
  public boolean isIssues() {
    return issues;
  }

  public boolean isPublish() {
    return !preview && !issues;
  }

  public GlobalMode(BootstrapProperties props) {
    String mode = props.property(CoreProperties.ANALYSIS_MODE);
    validate(mode);
    preview = CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
    issues = CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode);

    if (preview) {
      LOG.info("Preview global mode");
    } else if (issues) {
      LOG.info("Issues global mode");
    } else {
      LOG.info("Publish global mode");
    }
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

