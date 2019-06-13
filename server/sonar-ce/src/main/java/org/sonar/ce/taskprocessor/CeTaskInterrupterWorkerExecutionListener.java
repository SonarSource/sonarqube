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
package org.sonar.ce.taskprocessor;

import java.time.Duration;
import javax.annotation.Nullable;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskInterrupter;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.db.ce.CeActivityDto;

public class CeTaskInterrupterWorkerExecutionListener implements CeWorker.ExecutionListener {
  private final CeTaskInterrupter interrupter;

  public CeTaskInterrupterWorkerExecutionListener(CeTaskInterrupter interrupter) {
    this.interrupter = interrupter;
  }

  @Override
  public void onStart(CeTask ceTask) {
    interrupter.onStart(ceTask);
  }

  @Override
  public void onEnd(CeTask ceTask, CeActivityDto.Status status, Duration duration, @Nullable CeTaskResult taskResult, @Nullable Throwable error) {
    interrupter.onEnd(ceTask);
  }
}
