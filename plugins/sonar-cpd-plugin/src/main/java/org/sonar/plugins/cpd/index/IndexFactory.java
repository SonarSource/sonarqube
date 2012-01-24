/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cpd.index;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.duplication.DuplicationDao;

public class IndexFactory implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(IndexFactory.class);

  private final ResourcePersister resourcePersister;
  private final DuplicationDao dao;

  /**
   * For dry run, where is no access to database.
   */
  public IndexFactory() {
    this(null, null);
  }

  public IndexFactory(ResourcePersister resourcePersister, DuplicationDao dao) {
    this.resourcePersister = resourcePersister;
    this.dao = dao;
  }

  public SonarDuplicationsIndex create(Project project) {
    if (isCrossProject(project)) {
      LOG.info("Cross-project analysis enabled");
      return new SonarDuplicationsIndex(new DbDuplicationsIndex(resourcePersister, project, dao));
    } else {
      LOG.info("Cross-project analysis disabled");
      return new SonarDuplicationsIndex();
    }
  }

  /**
   * @return true, if was enabled by user and database is available
   */
  private boolean isCrossProject(Project project) {
    return project.getConfiguration().getBoolean(CoreProperties.CPD_CROSS_RPOJECT, CoreProperties.CPD_CROSS_RPOJECT_DEFAULT_VALUE)
      && resourcePersister != null && dao != null
      && StringUtils.isBlank(project.getConfiguration().getString(CoreProperties.PROJECT_BRANCH_PROPERTY));
  }

}
