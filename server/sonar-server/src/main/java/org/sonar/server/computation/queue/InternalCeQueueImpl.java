/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.queue;

import com.google.common.base.Optional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.utils.System2;
import org.sonar.ce.monitoring.CEQueueStatus;
import org.sonar.ce.queue.CeQueueImpl;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.queue.CeTaskResult;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@ComputeEngineSide
public class InternalCeQueueImpl extends CeQueueImpl implements InternalCeQueue {

  private final System2 system2;
  private final DbClient dbClient;
  private final CEQueueStatus queueStatus;

  // state
  private AtomicBoolean peekPaused = new AtomicBoolean(false);

  public InternalCeQueueImpl(System2 system2, DbClient dbClient, UuidFactory uuidFactory, CEQueueStatus queueStatus) {
    super(dbClient, uuidFactory);
    this.system2 = system2;
    this.dbClient = dbClient;
    this.queueStatus = queueStatus;
  }

  @Override
  public Optional<CeTask> peek() {
    if (peekPaused.get()) {
      return Optional.absent();
    }
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> dto = dbClient.ceQueueDao().peek(dbSession);
      CeTask task = null;
      if (dto.isPresent()) {
        task = loadTask(dbSession, dto.get());
        queueStatus.addInProgress();
      }
      return Optional.fromNullable(task);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @Override
  public int clear() {
    return cancelAll(true);
  }

  @Override
  public void remove(CeTask task, CeActivityDto.Status status, @Nullable CeTaskResult taskResult, @Nullable Throwable error) {
    checkArgument(error == null || status == CeActivityDto.Status.FAILED, "Error can be provided only when status is FAILED");
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, task.getUuid());
      checkState(queueDto.isPresent(), "Task does not exist anymore: %s", task);
      CeActivityDto activityDto = new CeActivityDto(queueDto.get());
      activityDto.setStatus(status);
      updateQueueStatus(status, activityDto);
      updateTaskResult(activityDto, taskResult);
      updateError(activityDto, error);
      remove(dbSession, queueDto.get(), activityDto);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static void updateTaskResult(CeActivityDto activityDto, @Nullable CeTaskResult taskResult) {
    if (taskResult != null) {
      java.util.Optional<String> analysisUuid = taskResult.getAnalysisUuid();
      if (analysisUuid.isPresent()) {
        activityDto.setAnalysisUuid(analysisUuid.get());
      }
    }
  }

  private static void updateError(CeActivityDto activityDto, @Nullable Throwable error) {
    if (error == null) {
      return;
    }

    activityDto.setErrorMessage(error.getMessage());
    String stacktrace = getStackTraceForPersistence(error);
    if (stacktrace != null) {
      activityDto.setErrorStacktrace(stacktrace);
    }
  }

  @CheckForNull
  private static String getStackTraceForPersistence(Throwable error) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
         LineReturnEnforcedPrintStream printStream = new LineReturnEnforcedPrintStream(out)) {
      error.printStackTrace(printStream);
      printStream.flush();
      return out.toString();
    } catch (IOException e) {
      Logger.getLogger(InternalCeQueueImpl.class).debug("Failed to getStacktrace out of error", e);
      return null;
    }
  }

  private void updateQueueStatus(CeActivityDto.Status status, CeActivityDto activityDto) {
    Long startedAt = activityDto.getStartedAt();
    if (startedAt == null) {
      return;
    }
    activityDto.setExecutedAt(system2.now());
    long executionTimeInMs = activityDto.getExecutedAt() - startedAt;
    activityDto.setExecutionTimeMs(executionTimeInMs);
    if (status == CeActivityDto.Status.SUCCESS) {
      queueStatus.addSuccess(executionTimeInMs);
    } else {
      queueStatus.addError(executionTimeInMs);
    }
  }

  @Override
  public void cancel(DbSession dbSession, CeQueueDto ceQueueDto) {
    cancelImpl(dbSession, ceQueueDto);
  }

  @Override
  public void pausePeek() {
    this.peekPaused.set(true);
  }

  @Override
  public void resumePeek() {
    this.peekPaused.set(false);
  }

  @Override
  public boolean isPeekPaused() {
    return peekPaused.get();
  }

  /**
   * A {@link PrintWriter} subclass which enforces that line returns are {@code \n} whichever the platform.
   */
  private static class LineReturnEnforcedPrintStream extends PrintWriter {

    LineReturnEnforcedPrintStream(OutputStream out) {
      super(out);
    }

    @Override
    public void println() {
      super.print('\n');
    }

    @Override
    public void println(boolean x) {
      super.print(x);
      println();
    }

    @Override
    public void println(char x) {
      super.print(x);
      println();
    }

    @Override
    public void println(int x) {
      super.print(x);
      println();
    }

    @Override
    public void println(long x) {
      super.print(x);
      println();
    }

    @Override
    public void println(float x) {
      super.print(x);
      println();
    }

    @Override
    public void println(double x) {
      super.print(x);
      println();
    }

    @Override
    public void println(char[] x) {
      super.print(x);
      println();
    }

    @Override
    public void println(String x) {
      super.print(x);
      println();
    }

    @Override
    public void println(Object x) {
      super.print(x);
      println();
    }
  }

}
