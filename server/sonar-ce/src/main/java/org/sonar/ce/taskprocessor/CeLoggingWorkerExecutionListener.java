/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.taskprocessor;

import javax.annotation.Nullable;
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.CeTaskResult;
import org.sonar.db.ce.CeActivityDto;

/**
 * {@link CeWorker.ExecutionListener} responsible of calling {@link CeLogging#initForTask(CeTask)} and
 * {@link CeLogging#clearForTask()}.
 */
public class CeLoggingWorkerExecutionListener implements CeWorker.ExecutionListener {
  private final CeLogging ceLogging;

  public CeLoggingWorkerExecutionListener(CeLogging ceLogging) {
    this.ceLogging = ceLogging;
  }

  @Override
  public void onStart(CeTask ceTask) {
    ceLogging.initForTask(ceTask);
  }

  @Override
  public void onEnd(CeTask ceTask, CeActivityDto.Status status, @Nullable CeTaskResult taskResult, @Nullable Throwable error) {
    ceLogging.clearForTask();
  }
}
