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
package org.sonar.core.persistence;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.Semaphores;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @since 3.5
 */
public class SemaphoreUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(SemaphoreUpdater.class);

  private SemaphoreDao dao;

  private Map<String, ScheduledExecutorService> handlers = Maps.newHashMap();

  public SemaphoreUpdater(SemaphoreDao dao) {
    this.dao = dao;
  }

  public void scheduleForUpdate(final Semaphores.Semaphore semaphore, int updatePeriodInSeconds) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Runnable updater = new Runnable() {
      public void run() {
        LOG.debug("Updating semaphore " + semaphore.getName());
        dao.update(semaphore);
      }
    };
    scheduler.scheduleWithFixedDelay(updater, updatePeriodInSeconds, updatePeriodInSeconds, TimeUnit.SECONDS);
    handlers.put(semaphore.getName(), scheduler);
  }

  public void stopUpdate(final String name) {
    if (handlers.containsKey(name)) {
      handlers.get(name).shutdownNow();
    }
  }
}
