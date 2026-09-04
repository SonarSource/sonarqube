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
package org.sonar.server.almsettings.ws;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.common.almsettings.telemetry.DevOpsConfigurationTelemetry;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.setting.ALM.AZURE_DEVOPS;
import static org.sonar.db.alm.setting.ALM.BITBUCKET;
import static org.sonar.db.alm.setting.ALM.BITBUCKET_CLOUD;

/**
 * Concurrent admin POSTs to {@code create_*} used to race past the Developer-Edition
 * single-configuration-per-ALM cap. The check-then-insert is now serialized by a JVM lock inside
 * {@link AlmSettingsSupport}, so on the single-node Developer Edition the invariant "at most one ALM
 * setting per family" holds even under contention.
 */
public class AlmSettingsCreationConcurrencyIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final MultipleAlmFeature multipleAlmFeature = mock(MultipleAlmFeature.class);
  private final DevOpsConfigurationTelemetry devOpsConfigurationTelemetry = mock(DevOpsConfigurationTelemetry.class);
  private final AzureDevOpsValidator azureDevOpsValidator = mock(AzureDevOpsValidator.class);

  // Both actions must share the same AlmSettingsSupport so they contend on the same JVM lock —
  // that matches production wiring where AlmSettingsSupport is a singleton.
  private final AlmSettingsSupport support = new AlmSettingsSupport(db.getDbClient(), userSession,
    new ComponentFinder(db.getDbClient(), null), multipleAlmFeature);

  private final WsActionTester azureWs = new WsActionTester(new CreateAzureAction(db.getDbClient(), userSession,
    support, devOpsConfigurationTelemetry, azureDevOpsValidator));
  private final WsActionTester bitbucketWs = new WsActionTester(new CreateBitBucketAction(db.getDbClient(), userSession,
    support, devOpsConfigurationTelemetry));
  private final WsActionTester bitbucketCloudWs = new WsActionTester(new CreateBitbucketCloudAction(db.getDbClient(), userSession,
    support, devOpsConfigurationTelemetry));

  private final ExecutorService executor = Executors.newFixedThreadPool(2);

  @Before
  public void before() {
    // Developer Edition — the only edition that enforces the single-configuration-per-ALM cap.
    when(multipleAlmFeature.isAvailable()).thenReturn(false);
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
  }

  @After
  public void after() {
    executor.shutdownNow();
  }

  @Test
  public void concurrent_azure_creation_never_exceeds_one_setting() throws InterruptedException {
    Outcome outcome = race(
      () -> azureWs.newRequest()
        .setParam("key", "azure-1")
        .setParam("personalAccessToken", "pat-1")
        .setParam("url", "https://ado.sonarqube.com/")
        .execute(),
      () -> azureWs.newRequest()
        .setParam("key", "azure-2")
        .setParam("personalAccessToken", "pat-2")
        .setParam("url", "https://ado.sonarqube.com/")
        .execute());

    assertThat(outcome.successCount.get()).isEqualTo(1);
    assertThat(outcome.failureCount.get()).isEqualTo(1);
    assertThat(outcome.failure.get())
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("A AZURE_DEVOPS setting is already defined");
    assertThat(db.getDbClient().almSettingDao().selectByAlm(db.getSession(), AZURE_DEVOPS)).hasSize(1);
  }

  @Test
  public void concurrent_bitbucket_family_creation_never_exceeds_one_setting() throws InterruptedException {
    // Bitbucket Server and Bitbucket Cloud share a single family cap — either persists, the other is rejected.
    Outcome outcome = race(
      () -> bitbucketWs.newRequest()
        .setParam("key", "bb-server")
        .setParam("url", "https://bitbucket.enterprise/")
        .setParam("personalAccessToken", "pat")
        .execute(),
      () -> bitbucketCloudWs.newRequest()
        .setParam("key", "bb-cloud")
        .setParam("workspace", "workspace")
        .setParam("clientId", "client-id")
        .setParam("clientSecret", "client-secret")
        .execute());

    assertThat(outcome.successCount.get()).isEqualTo(1);
    assertThat(outcome.failureCount.get()).isEqualTo(1);
    assertThat(outcome.failure.get())
      .isInstanceOf(BadRequestException.class)
      .satisfiesAnyOf(
        e -> assertThat(e).hasMessageContaining("A BITBUCKET setting is already defined"),
        e -> assertThat(e).hasMessageContaining("A BITBUCKET_CLOUD setting is already defined"));

    int bitbucketRows = db.getDbClient().almSettingDao().selectByAlm(db.getSession(), BITBUCKET).size();
    int bitbucketCloudRows = db.getDbClient().almSettingDao().selectByAlm(db.getSession(), BITBUCKET_CLOUD).size();
    assertThat(bitbucketRows + bitbucketCloudRows)
      .as("Bitbucket family cap violated: BITBUCKET=%d, BITBUCKET_CLOUD=%d", bitbucketRows, bitbucketCloudRows)
      .isEqualTo(1);
  }

  private Outcome race(Runnable requestA, Runnable requestB) throws InterruptedException {
    CyclicBarrier start = new CyclicBarrier(2);
    CountDownLatch done = new CountDownLatch(2);
    Outcome outcome = new Outcome();

    executor.submit(() -> attempt(start, done, requestA, outcome));
    executor.submit(() -> attempt(start, done, requestB, outcome));

    assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
    return outcome;
  }

  private static void attempt(CyclicBarrier start, CountDownLatch done, Runnable request, Outcome outcome) {
    try {
      start.await(10, TimeUnit.SECONDS);
      request.run();
      outcome.successCount.incrementAndGet();
    } catch (Exception e) {
      outcome.failureCount.incrementAndGet();
      outcome.failure.compareAndSet(null, e);
    } finally {
      done.countDown();
    }
  }

  private static final class Outcome {
    final AtomicInteger successCount = new AtomicInteger();
    final AtomicInteger failureCount = new AtomicInteger();
    final AtomicReference<Throwable> failure = new AtomicReference<>();
  }
}
