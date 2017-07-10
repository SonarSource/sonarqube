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
import com.sonar.orchestrator.http.HttpMethod;
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
import javax.annotation.Nullable;
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
  private static final int WAIT = 200; // ms
  private static final int MAX_WAIT_LOOP = 5 * 60 * 5; // 5 minutes

  private static final byte BLOCKING = (byte) 0x00;
  private static final byte UNLATCHED = (byte) 0x01;

  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static File sharedMemory;
  private static Orchestrator orchestrator;
  private static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() throws Exception {
    sharedMemory = temporaryFolder.newFile();

    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("fake-governance-plugin"))
      .setServerProperty("fakeGoverance.workerLatch.sharedMemoryFile", sharedMemory.getAbsolutePath())
      .addPlugin(xooPlugin());
    orchestrator = builder.build();
    orchestrator.start();

    adminWsClient = newAdminWsClient(orchestrator);
  }

  @AfterClass
  public static void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void enabled_worker_count_is_initially_1_and_can_be_changed_dynamically_by_plugin() throws IOException {
    assertThat(Files.lines(orchestrator.getServer().getCeLogs().toPath())
      .filter(s -> s.contains("Compute Engine will use ")))
        .isEmpty();

    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile = new RandomAccessFile(sharedMemory, "rw");
      MappedByteBuffer mappedByteBuffer = initMappedByteBuffer(randomAccessFile);

      verifyAnalysesRunInParallel(mappedByteBuffer, 1);

      /* 2 <= newWorkerCount <= 7 */
      int newWorkerCount = 4 + new Random().nextInt(4);
      updateWorkerCount(newWorkerCount);

      Set<String> line = Files.lines(orchestrator.getServer().getCeLogs().toPath())
        .filter(s -> s.contains("Compute Engine will use "))
        .collect(Collectors.toSet());
      assertThat(line).hasSize(1);
      assertThat(line.iterator().next()).contains(valueOf(newWorkerCount));

      verifyAnalysesRunInParallel(mappedByteBuffer, newWorkerCount);

      int lowerWorkerCount = 3;
      updateWorkerCount(lowerWorkerCount);
      verifyAnalysesRunInParallel(mappedByteBuffer, lowerWorkerCount);
    } finally {
      close(randomAccessFile);
    }
  }

  private void updateWorkerCount(int newWorkerCount) {
    orchestrator.getServer()
      .newHttpCall("api/ce/refreshWorkerCount")
      .setMethod(HttpMethod.POST)
      .setParam("count", valueOf(newWorkerCount))
      .execute();
  }

  private void verifyAnalysesRunInParallel(MappedByteBuffer mappedByteBuffer, int workerCount) {
    assertThat(adminWsClient.ce().workerCount())
      .extracting(WsCe.WorkerCountResponse::getValue, WsCe.WorkerCountResponse::getCanSetWorkerCount)
      .containsOnly(workerCount, true);

    blockAnyAnalysisWithFakeGovernancePlugin(mappedByteBuffer);

    // start analysis of workerCount + 2 projects
    List<String> projectKeys = IntStream.range(0, workerCount + 2).mapToObj(i -> "prj" + i).collect(toList());
    for (String projectKey : projectKeys) {
      SonarScanner sonarRunner = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
        .setProperties("sonar.projectKey", projectKey);
      orchestrator.executeBuild(sonarRunner, false);
    }

    List<WsCe.Task> tasksList = waitForWsCallStatus(
      this::getTasksAllTasks,
      (tasks) -> verifyInProgressTaskCount(tasks, workerCount));

    assertThat(tasksList.stream()
      .filter(CeWorkersTest::pending)
      .map(WsCe.Task::getComponentKey)
      .collect(toSet()))
        .isEqualTo(copyOf(projectKeys.subList(workerCount, projectKeys.size())));
    assertThat(tasksList.stream()
      .filter(CeWorkersTest::inProgress)
      .map(WsCe.Task::getComponentKey)
      .collect(toSet()))
        .isEqualTo(copyOf(projectKeys.subList(0, workerCount)));

    releaseAnyAnalysisWithFakeGovernancePlugin(mappedByteBuffer);

    waitForWsCallStatus(this::getTasksAllTasks, List::isEmpty);
  }

  private static MappedByteBuffer initMappedByteBuffer(RandomAccessFile randomAccessFile) throws IOException {
    return randomAccessFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1);
  }

  private void releaseAnyAnalysisWithFakeGovernancePlugin(MappedByteBuffer mappedByteBuffer) {
    // let the blocked analyses finish running
    mappedByteBuffer.put(0, UNLATCHED);
  }

  private static void blockAnyAnalysisWithFakeGovernancePlugin(MappedByteBuffer mappedByteBuffer) {
    // block any analysis which will run with the fake-governance-plugin
    mappedByteBuffer.put(0, BLOCKING);
  }

  private void close(@Nullable RandomAccessFile randomAccessFile) throws IOException {
    if (randomAccessFile != null) {
      randomAccessFile.close();
    }
  }

  private static boolean verifyInProgressTaskCount(List<WsCe.Task> tasksList, int workerCount) {
    return tasksList.stream().filter(CeWorkersTest::inProgress).count() >= workerCount;
  }

  private static boolean pending(WsCe.Task task) {
    return WsCe.TaskStatus.PENDING == task.getStatus();
  }

  private static boolean inProgress(WsCe.Task task) {
    return WsCe.TaskStatus.IN_PROGRESS == task.getStatus();
  }

  private List<WsCe.Task> getTasksAllTasks(WsClient wsClient) {
    return wsClient.ce().activity(new ActivityWsRequest()
      .setStatus(ImmutableList.of(STATUS_PENDING, STATUS_IN_PROGRESS)))
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
    assertThat(expectedState)
      .as("Failed to wait for expected queue status. Last call returned:\n%s", returnValue)
      .isTrue();
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
