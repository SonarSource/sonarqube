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
package org.sonar.server.pushapi.scheduler.polling;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.server.pushapi.sonarlint.SonarLintClient;
import org.sonar.server.pushapi.sonarlint.SonarLintClientsRegistry;
import org.sonar.server.pushapi.sonarlint.SonarLintPushEvent;
import org.sonar.server.util.AbstractStoppableExecutorService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushEventPollSchedulerTest {

  private final SonarLintClientsRegistry clientsRegistry = mock(SonarLintClientsRegistry.class);

  private static final long NOW = 1L;
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  private final Configuration config = mock(Configuration.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final SyncPushEventExecutorService executorService = new SyncPushEventExecutorService();

  @Test
  public void scheduler_should_be_resilient_to_failures() {
    when(clientsRegistry.getClients()).thenThrow(new RuntimeException("I have a bad feelings about this"));

    var underTest = new PushEventPollScheduler(executorService, clientsRegistry, db.getDbClient(), system2, config);
    underTest.start();

    assertThatCode(executorService::runCommand)
      .doesNotThrowAnyException();

    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));
  }

  @Test
  public void nothing_to_broadcast_when_client_list_is_empty() {
    when(clientsRegistry.getClients()).thenReturn(emptyList());

    var underTest = new PushEventPollScheduler(executorService, clientsRegistry, db.getDbClient(), system2, config);
    underTest.start();

    executorService.runCommand();

    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));
  }

  @Test
  public void nothing_to_broadcast_when_no_push_events() {
    var project = db.components().insertPrivateProject().getMainBranchComponent();

    var sonarLintClient = mock(SonarLintClient.class);
    when(sonarLintClient.getClientProjectUuids()).thenReturn(Set.of(project.uuid()));
    when(clientsRegistry.getClients()).thenReturn(List.of(sonarLintClient));

    var underTest = new PushEventPollScheduler(executorService, clientsRegistry, db.getDbClient(), system2, config);
    underTest.start();

    executorService.runCommand();

    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));
  }

  @Test
  public void nothing_to_broadcast_if_project_key_does_not_exist() {
    var project = db.components().insertPrivateProject().getMainBranchComponent();

    system2.setNow(1L);
    var sonarLintClient = mock(SonarLintClient.class);
    when(sonarLintClient.getClientProjectUuids()).thenReturn(Set.of("not-existing-project-uuid"));
    when(clientsRegistry.getClients()).thenReturn(List.of(sonarLintClient));

    var underTest = new PushEventPollScheduler(executorService, clientsRegistry, db.getDbClient(), system2, config);
    underTest.start();

    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));

    system2.tick(); // tick=2
    generatePushEvent(project.uuid());

    executorService.runCommand();

    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));
  }

  @Test
  public void broadcast_push_events() {
    var project = db.components().insertPrivateProject().getMainBranchComponent();

    system2.setNow(1L);
    var sonarLintClient = mock(SonarLintClient.class);
    when(sonarLintClient.getClientProjectUuids()).thenReturn(Set.of(project.uuid()));
    when(clientsRegistry.getClients()).thenReturn(List.of(sonarLintClient));

    var underTest = new PushEventPollScheduler(executorService, clientsRegistry, db.getDbClient(), system2, config);
    underTest.start();
    executorService.runCommand();

    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));

    system2.tick(); // tick=2
    generatePushEvent(project.uuid());
    generatePushEvent(project.uuid());

    system2.tick(); // tick=3
    generatePushEvent(project.uuid());

    underTest.start();
    executorService.runCommand();

    verify(clientsRegistry, times(3)).broadcastMessage(any(SonarLintPushEvent.class));

    system2.tick(); // tick=4
    generatePushEvent(project.uuid());
    generatePushEvent(project.uuid());

    underTest.start();
    executorService.runCommand();
    verify(clientsRegistry, times(5)).broadcastMessage(any(SonarLintPushEvent.class));
  }

  @Test
  public void broadcast_should_stop_polling_for_events_when_all_clients_unregister() {
    var project = db.components().insertPrivateProject().getMainBranchComponent();

    system2.setNow(1L);
    var sonarLintClient = mock(SonarLintClient.class);
    when(sonarLintClient.getClientProjectUuids()).thenReturn(Set.of(project.uuid()));
    when(clientsRegistry.getClients()).thenReturn(List.of(sonarLintClient), emptyList());

    var underTest = new PushEventPollScheduler(executorService, clientsRegistry, db.getDbClient(), system2, config);
    underTest.start();
    executorService.runCommand();

    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));

    system2.tick(); // tick=2
    generatePushEvent(project.uuid());

    underTest.start();
    executorService.runCommand();

    // all clients have been unregistered, nothing to broadcast
    verify(clientsRegistry, times(0)).broadcastMessage(any(SonarLintPushEvent.class));
  }

  private PushEventDto generatePushEvent(String projectUuid) {
    var event = db.getDbClient().pushEventDao().insert(db.getSession(), new PushEventDto()
      .setName("Event")
      .setUuid(UuidFactoryFast.getInstance().create())
      .setProjectUuid(projectUuid)
      .setPayload("some-event".getBytes(UTF_8)));
    db.commit();
    return event;
  }

  private static class SyncPushEventExecutorService extends AbstractStoppableExecutorService<ScheduledExecutorService>
    implements PushEventExecutorService {

    private Runnable command;

    public SyncPushEventExecutorService() {
      super(null);
    }

    public void runCommand() {
      command.run();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      this.command = command;
      return null;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      return null;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return null;
    }

  }

}
