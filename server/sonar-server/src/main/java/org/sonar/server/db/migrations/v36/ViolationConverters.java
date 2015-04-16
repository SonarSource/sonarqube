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
package org.sonar.server.db.migrations.v36;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.sonar.api.config.Settings;
import org.sonar.core.persistence.Database;

import com.google.common.collect.Lists;

class ViolationConverters {

  static final int DEFAULT_THREADS = 5;
  static final String THREADS_PROPERTY = "sonar.violationMigration.threads";
  private final Settings settings;

  ViolationConverters(Settings settings) {
    this.settings = settings;
  }

  void execute(Referentials referentials, Database db) {
    Progress progress = new Progress(referentials.totalViolations());

    List<Callable<Object>> converters = Lists.newArrayList();
    for (int i = 0; i < numberOfThreads(); i++) {
      converters.add(new ViolationConverter(referentials, db, progress));
    }

    doExecute(progress, converters);
  }

  void doExecute(TimerTask progress, List<Callable<Object>> converters) {
    Timer timer = new Timer(Progress.THREAD_NAME);
    timer.schedule(progress, Progress.DELAY_MS, Progress.DELAY_MS);
    try {
      ExecutorService executor = Executors.newFixedThreadPool(converters.size());
      List<Future<Object>> results = executor.invokeAll(converters);
      executor.shutdown();
      for (Future result : results) {
        result.get();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start migration threads", e);
    } finally {
      progress.cancel();
      timer.cancel();
      timer.purge();
    }
  }

  int numberOfThreads() {
    int threads = settings.getInt(THREADS_PROPERTY);
    if (threads < 0) {
      throw new IllegalArgumentException(String.format("Bad value of %s: %d", THREADS_PROPERTY, threads));
    }
    if (threads == 0) {
      threads = DEFAULT_THREADS;
    }
    return threads;
  }
}
