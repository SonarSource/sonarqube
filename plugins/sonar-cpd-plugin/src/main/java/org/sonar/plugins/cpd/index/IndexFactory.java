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
package org.sonar.plugins.cpd.index;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.duplication.DuplicationDao;

import javax.annotation.Nullable;

public class IndexFactory implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(IndexFactory.class);

  private final Settings settings;
  private final ResourcePersister resourcePersister;
  private final DuplicationDao dao;
  private final AnalysisMode mode;

  public IndexFactory(AnalysisMode mode, Settings settings, @Nullable ResourcePersister resourcePersister, @Nullable DuplicationDao dao) {
    this.mode = mode;
    this.settings = settings;
    this.resourcePersister = resourcePersister;
    this.dao = dao;
  }

  /**
   * Used by new sensor mode
   */
  public IndexFactory(AnalysisMode mode, Settings settings) {
    this(mode, settings, null, null);
  }

  public SonarDuplicationsIndex create(@Nullable Project project, String languageKey) {
    if (verifyCrossProject(project, LOG) && dao != null && resourcePersister != null) {
      return new SonarDuplicationsIndex(new DbDuplicationsIndex(resourcePersister, project, dao, languageKey));
    }
    return new SonarDuplicationsIndex();
  }

  @VisibleForTesting
  boolean verifyCrossProject(Project project, Logger logger) {
    boolean crossProject = false;

    if (settings.getBoolean(CoreProperties.CPD_CROSS_PROJECT)) {
      if (mode.isPreview()) {
        logger.info("Cross-project analysis disabled. Not supported in preview mode.");
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
