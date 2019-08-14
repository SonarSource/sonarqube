/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualitygate;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresQuery;
import org.sonar.server.util.GlobalLockManager;

import static org.sonar.api.measures.Metric.Level.WARN;

/**
 * This class is regularly checking the number of projects in warning state, in order to not return the "Warning" value in the quality gate facet of the Projects page when there are no more projects in warning.
 * 
 * @see <a href="https://jira.sonarsource.com/browse/SONAR-12140">SONAR-12140</a> for more information
 */
public class ProjectsInWarningDaemon implements Startable {

  final static String PROJECTS_IN_WARNING_INTERNAL_PROPERTY = "projectsInWarning";

  private static final Logger LOG = Loggers.get(ProjectsInWarningDaemon.class);

  private static final String FREQUENCY_IN_MILLISECONDS_PROPERTY = "sonar.projectsInWarning.frequencyInMilliseconds";
  private static final int DEFAULT_FREQUENCY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;
  private static final String THREAD_NAME_PREFIX = "sq-projects-in-warning-service-";

  private static final String LOCK_NAME = "ProjectsInWarn";
  private static final int LOCK_DURATION_IN_SECOND = 60 * 60;

  private final DbClient dbClient;
  private final ProjectMeasuresIndex projectMeasuresIndex;
  private final Configuration config;
  private final GlobalLockManager lockManager;
  private final ProjectsInWarning projectsInWarning;

  private ScheduledExecutorService executorService;

  public ProjectsInWarningDaemon(DbClient dbClient, ProjectMeasuresIndex projectMeasuresIndex, Configuration config, GlobalLockManager lockManager,
    ProjectsInWarning projectsInWarning) {
    this.dbClient = dbClient;
    this.projectMeasuresIndex = projectMeasuresIndex;
    this.config = config;
    this.lockManager = lockManager;
    this.projectsInWarning = projectsInWarning;
  }

  public void notifyStart() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<String> internalProperty = dbClient.internalPropertiesDao().selectByKey(dbSession, PROJECTS_IN_WARNING_INTERNAL_PROPERTY);
      if (internalProperty.isPresent() && internalProperty.get().equals("0")) {
        projectsInWarning.update(0L);
        LOG.info("Counting number of projects in warning is not started as there are no projects in this situation.");
        return;
      }
    }
    LOG.info("Counting number of projects in warning is enabled.");
    executorService = Executors.newSingleThreadScheduledExecutor(newThreadFactory());
    executorService.scheduleWithFixedDelay(countProjectsInWarning(), 0, frequency(), TimeUnit.MILLISECONDS);
  }

  private int frequency() {
    return config.getInt(FREQUENCY_IN_MILLISECONDS_PROPERTY).orElse(DEFAULT_FREQUENCY_IN_MILLISECONDS);
  }

  private Runnable countProjectsInWarning() {
    return () -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        long nbProjectsInWarning = projectMeasuresIndex.search(
          new ProjectMeasuresQuery()
            .setQualityGateStatus(WARN)
            .setIgnoreAuthorization(true),
          // We only need the number of projects in warning
          new SearchOptions().setLimit(1)).getTotal();
        projectsInWarning.update(nbProjectsInWarning);
        updateProjectsInWarningInDb(dbSession, nbProjectsInWarning);
        if (nbProjectsInWarning == 0L) {
          LOG.info("Counting number of projects in warning will be disabled as there are no more projects in warning.");
          executorService.shutdown();
        }
      } catch (Exception e) {
        LOG.error("Error while counting number of projects in warning: {}", e);
      }
    };
  }

  private void updateProjectsInWarningInDb(DbSession dbSession, long nbProjectsInWarning) {
    // Only one web node should do the update in db to avoid any collision
    if (!lockManager.tryLock(LOCK_NAME, LOCK_DURATION_IN_SECOND)) {
      return;
    }
    dbClient.internalPropertiesDao().save(dbSession, PROJECTS_IN_WARNING_INTERNAL_PROPERTY, Long.toString(nbProjectsInWarning));
    dbSession.commit();
  }

  @Override
  public void start() {
    // Nothing is done here, as this component needs to be started after ES indexing. See PlatformLevelStartup for more info.
  }

  @Override
  public void stop() {
    if (executorService == null) {
      return;
    }
    try {
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static ThreadFactory newThreadFactory() {
    return new ThreadFactoryBuilder()
      .setNameFormat(THREAD_NAME_PREFIX + "%d")
      .setPriority(Thread.MIN_PRIORITY)
      .build();
  }

}
