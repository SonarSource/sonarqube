/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.db.migrations.violation;

import org.sonar.core.persistence.Database;

import java.util.Timer;
import java.util.concurrent.*;

class ViolationConverters {

  static final int MAX_THREADS = 5;

  private final ExecutorService executorService;
  private final Database database;
  private final Referentials referentials;
  private final Progress progress;
  private final Timer timer;

  ViolationConverters(Database db, Referentials referentials) {
    this.database = db;
    this.referentials = referentials;

    this.progress = new Progress(referentials.totalViolations());
    timer = new Timer(Progress.THREAD_NAME);
    timer.schedule(progress, Progress.DELAY_MS, Progress.DELAY_MS);

    BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(MAX_THREADS);
    RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
    this.executorService = new ThreadPoolExecutor(0, MAX_THREADS, 5L, TimeUnit.SECONDS, blockingQueue, rejectedExecutionHandler);
  }

  void convert(Object[] violationIds) {
    executorService.execute(new ViolationConverter(referentials, database, violationIds, progress));
  }

  void waitForFinished() throws InterruptedException {
    executorService.shutdown();
    executorService.awaitTermination(10L, TimeUnit.SECONDS);
    timer.cancel();
  }
}
