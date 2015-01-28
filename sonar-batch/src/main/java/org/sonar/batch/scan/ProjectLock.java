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
package org.sonar.batch.scan;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Semaphores;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;

import java.util.Locale;

public class ProjectLock {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectLock.class);

  private final Semaphores semaphores;
  private final ProjectTree projectTree;
  private final DefaultAnalysisMode analysisMode;
  private final I18n i18n;

  public ProjectLock(Semaphores semaphores, ProjectTree projectTree, DefaultAnalysisMode analysisMode, I18n i18n) {
    this.semaphores = semaphores;
    this.projectTree = projectTree;
    this.analysisMode = analysisMode;
    this.i18n = i18n;
  }

  public void start() {
    if (!analysisMode.isPreview() && StringUtils.isNotBlank(getProject().getKey())) {
      Semaphores.Semaphore semaphore = acquire();
      if (!semaphore.isLocked()) {
        LOG.error(getErrorMessage(semaphore));
        throw new SonarException("The project is already being analysed.");
      }
    }
  }

  private String getErrorMessage(Semaphores.Semaphore semaphore) {
    long duration = semaphore.getDurationSinceLocked();
    String durationDisplay = i18n.age(Locale.ENGLISH, duration);

    return "It looks like an analysis of '" + getProject().getName() + "' is already running (started " + durationDisplay + " ago).";
  }

  public void stop() {
    if (!analysisMode.isPreview()) {
      release();
    }
  }

  private Semaphores.Semaphore acquire() {
    LOG.debug("Acquire semaphore on project : {}, with key {}", getProject(), getSemaphoreKey());
    return semaphores.acquire(getSemaphoreKey(), 15, 10);
  }

  private void release() {
    LOG.debug("Release semaphore on project : {}, with key {}", getProject(), getSemaphoreKey());
    semaphores.release(getSemaphoreKey());
  }

  private String getSemaphoreKey() {
    return "batch-" + getProject().getKey();
  }

  private Project getProject() {
    return projectTree.getRootProject();
  }
}
