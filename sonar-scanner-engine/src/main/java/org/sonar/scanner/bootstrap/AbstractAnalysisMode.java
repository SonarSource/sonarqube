/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;

import java.util.Arrays;

import org.sonar.api.batch.AnalysisMode;

public abstract class AbstractAnalysisMode implements AnalysisMode {
  private static final String[] VALID_MODES = {CoreProperties.ANALYSIS_MODE_PREVIEW, CoreProperties.ANALYSIS_MODE_PUBLISH, CoreProperties.ANALYSIS_MODE_ISSUES};

  protected boolean preview;
  protected boolean issues;

  protected AbstractAnalysisMode() {
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

  protected static void validate(String mode) {
    if (StringUtils.isEmpty(mode)) {
      return;
    }

    if (CoreProperties.ANALYSIS_MODE_INCREMENTAL.equals(mode)) {
      throw new IllegalStateException("Invalid analysis mode: " + mode + ". This mode was removed in SonarQube 5.2. Valid modes are: " + Arrays.toString(VALID_MODES));
    }

    if (!Arrays.asList(VALID_MODES).contains(mode)) {
      throw new IllegalStateException("Invalid analysis mode: " + mode + ". Valid modes are: " + Arrays.toString(VALID_MODES));
    }
  }

}
