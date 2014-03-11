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
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.duplication.DuplicationDao;

public class IndexFactory implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(IndexFactory.class);

  private final Settings settings;
  private final ResourcePersister resourcePersister;
  private final DuplicationDao dao;

  public IndexFactory(Settings settings, ResourcePersister resourcePersister, DuplicationDao dao) {
    this.settings = settings;
    this.resourcePersister = resourcePersister;
    this.dao = dao;
  }

  public SonarDuplicationsIndex create(Project project) {
    if (verifyCrossProject(project, LOG)) {
      return new SonarDuplicationsIndex(new DbDuplicationsIndex(resourcePersister, project, dao));
    }
    return new SonarDuplicationsIndex();
  }

  @VisibleForTesting
  boolean verifyCrossProject(Project project, Logger logger) {
    boolean crossProject = false;

    if (settings.getBoolean(CoreProperties.CPD_CROSS_RPOJECT)) {
      if (settings.getBoolean(CoreProperties.DRY_RUN)) {
        logger.info("Cross-project analysis disabled. Not supported on dry runs.");
      } else if (StringUtils.isNotBlank(project.getBranch())) {
        logger.info("Cross-project analysis disabled. Not supported on project branches.");
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
