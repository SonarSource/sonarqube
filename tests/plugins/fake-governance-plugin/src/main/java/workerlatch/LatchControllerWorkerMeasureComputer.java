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
package workerlatch;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class LatchControllerWorkerMeasureComputer implements MeasureComputer {
  private static final Logger LOG = Loggers.get(LatchControllerWorkerMeasureComputer.class);
  private static final String WORKER_LATCH_SHARED_MEMORY_FILE_PATH = "fakeGoverance.workerLatch.sharedMemoryFile";

  private static final int WAIT = 200; // ms
  private static final int MAX_WAIT_ROUND = 5 * 60 * 2; // 2 minutes

  private static final int MAX_SHARED_MEMORY = 1;
  private static final byte UNLATCHED = (byte) 0x01;

  private final File sharedMemory;

  public LatchControllerWorkerMeasureComputer(Configuration configuration) {
    this.sharedMemory = configuration.get(WORKER_LATCH_SHARED_MEMORY_FILE_PATH)
      .map(path -> {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
          return file;
        }
        LOG.info("Latch sharedMemory file {} is not a file or does not exist", path);
        return null;
      })
      .orElse(null);
  }

  private boolean isLatchEnabled() {
    return sharedMemory != null;
  }

  @Override
  public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
    return defContext.newDefinitionBuilder()
      .setOutputMetrics(WorkerLatchMetrics.METRIC_KEY)
      .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    Component component = context.getComponent();
    if (isLatchEnabled() && component.getType() == Component.Type.PROJECT) {
      context.addMeasure(WorkerLatchMetrics.METRIC_KEY, waitForUnlatched(component.getKey()));
    } else {
      context.addMeasure(WorkerLatchMetrics.METRIC_KEY, false);
    }
  }

  private boolean waitForUnlatched(String key) {
    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile = new RandomAccessFile(sharedMemory, "rw");
      MappedByteBuffer mappedByteBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, MAX_SHARED_MEMORY);
      int i = 0;
      boolean unlatched = isUnlatched(mappedByteBuffer);
      while (!unlatched && i < MAX_WAIT_ROUND) {
        waitInterruptedly();
        i++;
        unlatched = isUnlatched(mappedByteBuffer);
      }
      LOG.info("Project {} unlatched={} i={}",  key, unlatched, i);

      return true;
    } catch (IOException e) {
      LOG.error("Failed to read or write to shared memory", e);
      return false;
    } finally {
      if (randomAccessFile != null) {
        try {
          randomAccessFile.close();
        } catch (IOException e) {
          LOG.error("Failed to close randomAccessFile", e);
        }
      }
    }
  }

  private static void waitInterruptedly() {
    try {
      Thread.sleep(WAIT);
    } catch (InterruptedException e) {
      LOG.error("Wait was interrupted");
    }
  }

  private boolean isUnlatched(MappedByteBuffer mappedByteBuffer) {
    return mappedByteBuffer.get(0) == UNLATCHED;
  }

}
