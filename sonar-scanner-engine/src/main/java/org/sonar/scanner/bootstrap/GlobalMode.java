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
package org.sonar.scanner.bootstrap;

import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;

@Immutable
public class GlobalMode extends AbstractAnalysisMode {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalMode.class);

  public GlobalMode(GlobalProperties props) {
    String mode = props.property(CoreProperties.ANALYSIS_MODE);
    validate(mode);
    issues = CoreProperties.ANALYSIS_MODE_ISSUES.equals(mode) || CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode);
    mediumTestMode = "true".equals(props.property(MEDIUM_TEST_ENABLED));
    if (preview) {
      LOG.debug("Preview global mode");
    } else if (issues) {
      LOG.debug("Issues global mode");
    } else {
      LOG.debug("Publish global mode");
    }
    if (mediumTestMode) {
      LOG.info("Medium test mode");
    }
  }

}
