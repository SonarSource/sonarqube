/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.telemetry.gessie;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.sonar.server.util.AbstractStoppableExecutorService;
import org.sonarsource.gessie.server.telemetry.GessieIngestorWorkers;

/**
 * Fixed thread pool for the Gessie ingestor workers. Provides daemon threads with
 * the sonar-enterprise stop/lifecycle contract so the container shuts it down cleanly.
 */
public class GessieIngestorExecutorServiceImpl extends AbstractStoppableExecutorService<ExecutorService>
  implements GessieIngestorWorkers {

  private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

  public GessieIngestorExecutorServiceImpl() {
    super(Executors.newFixedThreadPool(
      Runtime.getRuntime().availableProcessors(),
      GessieIngestorExecutorServiceImpl::createThread));
  }

  static Thread createThread(Runnable r) {
    Thread thread = Executors.defaultThreadFactory().newThread(r);
    thread.setName("gessie-ingestor-" + THREAD_COUNTER.getAndIncrement());
    thread.setDaemon(true);
    return thread;
  }
}
