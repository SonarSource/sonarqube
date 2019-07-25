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
package org.sonar.ce.queue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.container.ComputeEngineStatus;
import org.sonar.ce.monitoring.CEQueueStatus;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.TypedException;
import org.sonar.ce.task.projectanalysis.component.VisitException;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDao;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

@ComputeEngineSide
public class InternalCeQueueImpl extends CeQueueImpl implements InternalCeQueue {
  private static final Logger LOG = Loggers.get(InternalCeQueueImpl.class);

  private final DbClient dbClient;
  private final CEQueueStatus queueStatus;
  private final ComputeEngineStatus computeEngineStatus;

  public InternalCeQueueImpl(System2 system2, DbClient dbClient, UuidFactory uuidFactory, CEQueueStatus queueStatus,
    DefaultOrganizationProvider defaultOrganizationProvider, ComputeEngineStatus computeEngineStatus) {
    super(system2, dbClient, uuidFactory, defaultOrganizationProvider);
    this.dbClient = dbClient;
    this.queueStatus = queueStatus;
    this.computeEngineStatus = computeEngineStatus;
  }

  @Override
  public Optional<CeTask> peek(String workerUuid) {
    requireNonNull(workerUuid, "workerUuid can't be null");

    if (computeEngineStatus.getStatus() != ComputeEngineStatus.Status.STARTED || getWorkersPauseStatus() != WorkersPauseStatus.RESUMED) {
      return Optional.empty();
    }
    try (DbSession dbSession = dbClient.openSession(false)) {
      CeQueueDao ceQueueDao = dbClient.ceQueueDao();
      int i = ceQueueDao.resetToPendingForWorker(dbSession, workerUuid);
      if (i > 0) {
        dbSession.commit();
        LOG.debug("{} in progress tasks reset for worker uuid {}", i, workerUuid);
      }
      Optional<CeQueueDto> opt = ceQueueDao.peek(dbSession, workerUuid);
      if (opt.isPresent()) {
        CeQueueDto taskDto = opt.get();
        Map<String, ComponentDto> componentsByUuid = loadComponentDtos(dbSession, taskDto);
        Map<String, String> characteristics = dbClient.ceTaskCharacteristicsDao().selectByTaskUuids(dbSession, singletonList(taskDto.getUuid())).stream()
          .collect(uniqueIndex(CeTaskCharacteristicDto::getKey, CeTaskCharacteristicDto::getValue));

        CeTask task = convertToTask(dbSession, taskDto, characteristics,
          ofNullable(taskDto.getComponentUuid()).map(componentsByUuid::get).orElse(null),
          ofNullable(taskDto.getMainComponentUuid()).map(componentsByUuid::get).orElse(null));
        queueStatus.addInProgress();
        return Optional.of(task);
      }
      return Optional.empty();
    }
  }

  @Override
  public void remove(CeTask task, CeActivityDto.Status status, @Nullable CeTaskResult taskResult, @Nullable Throwable error) {
    checkArgument(error == null || status == CeActivityDto.Status.FAILED, "Error can be provided only when status is FAILED");

    long executionTimeInMs = 0L;
    try (DbSession dbSession = dbClient.openSession(false)) {
      CeQueueDto queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, task.getUuid())
        .orElseThrow(() -> new IllegalStateException("Task does not exist anymore: " + task));
      CeActivityDto activityDto = new CeActivityDto(queueDto);
      activityDto.setStatus(status);
      executionTimeInMs = updateExecutionFields(activityDto);
      updateTaskResult(activityDto, taskResult);
      updateError(activityDto, error);
      remove(dbSession, queueDto, activityDto);
    } finally {
      updateQueueStatus(status, executionTimeInMs);
    }
  }

  private void updateQueueStatus(CeActivityDto.Status status, long executionTimeInMs) {
    if (status == CeActivityDto.Status.SUCCESS) {
      queueStatus.addSuccess(executionTimeInMs);
    } else {
      queueStatus.addError(executionTimeInMs);
    }
  }

  private static void updateTaskResult(CeActivityDto activityDto, @Nullable CeTaskResult taskResult) {
    if (taskResult != null) {
      Optional<String> analysisUuid = taskResult.getAnalysisUuid();
      analysisUuid.ifPresent(activityDto::setAnalysisUuid);
    }
  }

  private static void updateError(CeActivityDto activityDto, @Nullable Throwable error) {
    if (error == null) {
      return;
    }

    if (error instanceof VisitException && error.getCause() != null) {
      activityDto.setErrorMessage(format("%s (%s)", error.getCause().getMessage(), error.getMessage()));
    } else {
      activityDto.setErrorMessage(error.getMessage());
    }
    String stacktrace = getStackTraceForPersistence(error);
    if (stacktrace != null) {
      activityDto.setErrorStacktrace(stacktrace);
    }
    if (error instanceof TypedException) {
      activityDto.setErrorType(((TypedException) error).getType());
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
      LOG.debug("Failed to getStacktrace out of error", e);
      return null;
    }
  }

  @Override
  public void cancelWornOuts() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<CeQueueDto> wornOutTasks = dbClient.ceQueueDao().selectWornout(dbSession);
      wornOutTasks.forEach(queueDto -> {
        CeActivityDto activityDto = new CeActivityDto(queueDto);
        activityDto.setStatus(CeActivityDto.Status.CANCELED);
        updateExecutionFields(activityDto);
        remove(dbSession, queueDto, activityDto);
      });
    }
  }

  @Override
  public void resetTasksWithUnknownWorkerUUIDs(Set<String> knownWorkerUUIDs) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.ceQueueDao().resetTasksWithUnknownWorkerUUIDs(dbSession, knownWorkerUUIDs);
      dbSession.commit();
    }
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
