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
package org.sonarqube.tests.ce;

import com.google.common.util.concurrent.Uninterruptibles;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.client.ce.ActivityStatusRequest;
import util.XooProjectBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class CeWorkersPauseTest {
  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(600));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR).disableOrganizations();

  @After
  public void tearDown() {
    tester.wsClient().ce().resume();
  }

  @Test
  public void pause_and_resume_workers() throws IOException {
    tester.wsClient().ce().pause();
    // no in-progress tasks --> already paused
    assertThat(tester.wsClient().ce().info().getWorkersPaused()).isTrue();
    assertThat(tester.wsClient().ce().info().getWorkersPauseRequested()).isFalse();

    // run analysis
    File projectDir = temp.newFolder();
    new XooProjectBuilder("sample").build(projectDir);
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir), false);

    // analysis is not processed by Compute Engine
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    Ce.ActivityStatusWsResponse activity = getActivityStatuses();
    assertThat(activity.getPending()).isEqualTo(1);
    assertThat(activity.getInProgress()).isEqualTo(0);

    // workers are resumed
    tester.wsClient().ce().resume();
    assertThat(tester.wsClient().ce().info().getWorkersPaused()).isFalse();
    assertThat(tester.wsClient().ce().info().getWorkersPauseRequested()).isFalse();

    while (!isQueueEmpty()) {
      Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    }
  }

  private Ce.ActivityStatusWsResponse getActivityStatuses() {
    return tester.wsClient().ce().activityStatus(new ActivityStatusRequest().setComponentKey("sample"));
  }

  private boolean isQueueEmpty() {
    Ce.ActivityStatusWsResponse activity = getActivityStatuses();
    return activity.getPending() == 0 && activity.getInProgress() == 0;
  }
}
