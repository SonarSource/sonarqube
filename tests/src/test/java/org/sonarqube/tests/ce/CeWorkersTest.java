/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.ce;

import com.google.common.collect.ImmutableList;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.ce.ActivityWsRequest;
import util.ItUtils;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

public class CeWorkersTest {
  /** 2 <= workerCount <= 5 */
  private static final int WORKER_COUNT = 2 + new Random().nextInt(4);

  private static final int WAIT = 200; // ms
  private static final int MAX_WAIT_LOOP = 5 * 10; // 10s

  private static final byte BLOCKING = (byte) 0x00;
  private static final byte UNLATCHED = (byte) 0x01;

  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
  private static final String STATUS_SUCCESS = "SUCCESS";
  private static final String STATUS_FAILED = "FAILED";

  private static final String STATUS_CANCELED = "CANCELED";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static File sharedMemory;
  private static Orchestrator orchestrator;

  @BeforeClass
  public static void setUp() throws Exception {
    sharedMemory = temporaryFolder.newFile();

    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("fake-governance-plugin"))
      .setServerProperty("fakeGovernance.workerCount", valueOf(WORKER_COUNT))
      .setServerProperty("fakeGoverance.workerLatch.sharedMemoryFile", sharedMemory.getAbsolutePath())
      .addPlugin(xooPlugin());
    orchestrator = builder.build();
    orchestrator.start();
  }

  @AfterClass
  public static void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void workerCount_is_modified_by_plugin() throws IOException {
    Set<String> line = Files.lines(orchestrator.getServer().getCeLogs().toPath())
      .filter(s -> s.contains("Compute Engine will use "))
      .collect(Collectors.toSet());
    assertThat(line)
      .hasSize(1);
    assertThat(line.iterator().next()).contains(valueOf(WORKER_COUNT));

    assertThat(newAdminWsClient(orchestrator).ce().workerCount())
      .extracting(WsCe.WorkerCountResponse::getValue, WsCe.WorkerCountResponse::getCanSetWorkerCount)
      .containsOnly(WORKER_COUNT, true);
  }

  @Test
  public void number_of_analysis_processed_in_parallel_is_equal_to_number_of_workers() throws IOException {
    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile = new RandomAccessFile(sharedMemory, "rw");
      MappedByteBuffer mappedByteBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1);
      // block any analysis which will run with the fake-governance-plugin
      mappedByteBuffer.put(0, BLOCKING);

      // start analysis of WORKER_COUNT + 2 projects
      List<String> projectKeys = IntStream.range(0, WORKER_COUNT + 2).mapToObj(i -> "prj" + i).collect(toList());
      for (String projectKey : projectKeys) {
        SonarScanner sonarRunner = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
          .setProperties("sonar.projectKey", projectKey);
        orchestrator.executeBuild(sonarRunner, false);
      }

      List<WsCe.Task> tasksList = waitForWsCallStatus(this::getTasksAllTasks, CeWorkersTest::threeTasksNotPending);
      boolean expectedState = threeTasksNotPending(tasksList);
      // let the blocked analyses finish running
      mappedByteBuffer.put(0, UNLATCHED);

      assertThat(expectedState).as("Couldn't get to the expected CE queue state in time").isTrue();
      assertThat(tasksList.stream()
        .filter(CeWorkersTest::pending)
        .map(WsCe.Task::getComponentKey)
        .collect(toSet()))
          .isEqualTo(copyOf(projectKeys.subList(WORKER_COUNT, projectKeys.size())));
      assertThat(tasksList.stream()
        .filter(CeWorkersTest::inProgress)
        .map(WsCe.Task::getComponentKey)
        .collect(toSet()))
          .isEqualTo(copyOf(projectKeys.subList(0, WORKER_COUNT)));

      waitForWsCallStatus(this::getTasksAllTasks, tasks -> tasks.stream().noneMatch(CeWorkersTest::pending));
    } finally {
      if (randomAccessFile != null) {
        randomAccessFile.close();
      }
    }
  }

  private static boolean threeTasksNotPending(List<WsCe.Task> tasksList) {
    return tasksList.stream().filter(task -> !pending(task)).count() >= WORKER_COUNT;
  }

  private static boolean pending(WsCe.Task task) {
    return WsCe.TaskStatus.PENDING == task.getStatus();
  }

  private static boolean inProgress(WsCe.Task task) {
    return WsCe.TaskStatus.IN_PROGRESS == task.getStatus();
  }

  private List<WsCe.Task> getTasksAllTasks(WsClient wsClient) {
    return wsClient.ce().activity(new ActivityWsRequest()
      .setStatus(ImmutableList.of(STATUS_PENDING, STATUS_IN_PROGRESS, STATUS_SUCCESS, STATUS_FAILED, STATUS_CANCELED)))
      .getTasksList();
  }

  private <T> T waitForWsCallStatus(Function<WsClient, T> call, Function<T, Boolean> test) {
    WsClient wsClient = ItUtils.newAdminWsClient(orchestrator);
    int i = 0;
    T returnValue = call.apply(wsClient);
    boolean expectedState = test.apply(returnValue);
    while (i < MAX_WAIT_LOOP && !expectedState) {
      waitInterruptedly();
      i++;
      returnValue = call.apply(wsClient);
      expectedState = test.apply(returnValue);
    }
    return returnValue;
  }

  private static void waitInterruptedly() {
    try {
      Thread.sleep(WAIT);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
