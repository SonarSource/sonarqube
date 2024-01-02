/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

public class AsyncExecutionImpl implements AsyncExecution {
  private static final Logger LOG = LoggerFactory.getLogger(AsyncExecutionImpl.class);
  private final AsyncExecutionExecutorService executorService;

  public AsyncExecutionImpl(AsyncExecutionExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void addToQueue(Runnable r) {
    requireNonNull(r);
    executorService.addToQueue(() -> {
      try {
        r.run();
      } catch (Exception e) {
        LOG.error("Asynchronous task failed", e);
      }
    });
  }
}
