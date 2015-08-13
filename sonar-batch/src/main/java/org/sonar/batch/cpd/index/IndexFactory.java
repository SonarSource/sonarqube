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
package org.sonar.batch.cpd.index;

import org.sonar.batch.analysis.DefaultAnalysisMode;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

@BatchSide
public class IndexFactory {

  private final Settings settings;
  private final DefaultAnalysisMode mode;

  public IndexFactory(DefaultAnalysisMode mode, Settings settings) {
    this.mode = mode;
    this.settings = settings;
  }

  public SonarDuplicationsIndex create(@Nullable Project project, String languageKey) {
    return new SonarDuplicationsIndex();
  }

  @VisibleForTesting
  boolean verifyCrossProject(@Nullable Project project, Logger logger) {
    boolean crossProject = false;

    if (settings.getBoolean(CoreProperties.CPD_CROSS_PROJECT)) {
      if (mode.isIssues()) {
        logger.info("Cross-project analysis disabled. Not supported in issues mode.");
      } else if (StringUtils.isNotBlank(settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY))) {
        logger.info("Cross-project analysis disabled. Not supported on project branches.");
      } else if (project == null) {
        // New sensor mode
        logger.info("Cross-project analysis disabled. Not supported in new sensor mode.");
      } else {
        logger.info("Cross-project analysis enabled");
        crossProject = true;
      }
    } else {
      logger.info("Cross-project analysis disabled");
    }
    return crossProject;
  }
}
