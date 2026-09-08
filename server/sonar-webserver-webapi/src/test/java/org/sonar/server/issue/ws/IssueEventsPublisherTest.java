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
package org.sonar.server.issue.ws;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.IssueStatus;
import org.sonarsource.issueprocessing.events.workflow.generated.IssueStatusUpdatedEvent;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.EventAsyncClient;
import org.sonarsource.sonarqube.events.api.EventSource;
import org.sonarsource.sonarqube.events.api.EventSourceBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IssueEventsPublisherTest {

  private final EventAsyncClient eventAsyncClient = mock(EventAsyncClient.class);
  private final EventSourceBuilder eventSourceBuilder = mock(EventSourceBuilder.class);

  private final IssueEventsPublisher underTest = new IssueEventsPublisher(eventAsyncClient, eventSourceBuilder);

  @Test
  void publishStatusUpdated_publishesWorkflowIssueStatusUpdatedEvent() {
    when(eventSourceBuilder.build(IssueEventsPublisher.SOURCE_DOMAIN, IssueEventsPublisher.SOURCE_SERVICE))
      .thenReturn(new EventSource(IssueEventsPublisher.SOURCE_DOMAIN, IssueEventsPublisher.SOURCE_SERVICE, null, null, null));
    when(eventAsyncClient.publishCrossDomainEvents(any())).thenReturn(CompletableFuture.completedFuture(null));

    underTest.publishStatusUpdated("ha-issue-key", IssueStatus.ACCEPTED);

    ArgumentCaptor<Collection<Event<?>>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(eventAsyncClient).publishCrossDomainEvents(captor.capture());

    assertThat(captor.getValue()).hasSize(1);
    Event<?> event = captor.getValue().iterator().next();
    assertThat(event.metadata().eventType()).isEqualTo(IssueEventsPublisher.EVENT_TYPE);
    assertThat(event.metadata().eventVersion()).isEqualTo(IssueEventsPublisher.EVENT_VERSION);
    assertThat(event.payload()).isInstanceOfSatisfying(IssueStatusUpdatedEvent.class, payload -> {
      assertThat(payload.getIssueId()).isEqualTo("ha-issue-key");
      assertThat(payload.getStatus().name()).isEqualTo("ACCEPTED");
    });
  }

  @Test
  void publishStatusUpdated_batchesMultipleIssuesIntoASinglePublishCall() {
    when(eventSourceBuilder.build(any(), any())).thenReturn(new EventSource("d", "s", null, null, null));
    when(eventAsyncClient.publishCrossDomainEvents(any())).thenReturn(CompletableFuture.completedFuture(null));

    underTest.publishStatusUpdated(Map.of("issue-1", IssueStatus.ACCEPTED, "issue-2", IssueStatus.FIXED));

    ArgumentCaptor<Collection<Event<?>>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(eventAsyncClient).publishCrossDomainEvents(captor.capture());
    assertThat(captor.getValue()).hasSize(2);
  }

  @Test
  void publishStatusUpdated_withEmptyMap_doesNotCallClient() {
    underTest.publishStatusUpdated(Map.of());

    verifyNoInteractions(eventAsyncClient);
  }

  @Test
  void publishStatusUpdated_withInSandboxStatus_doesNotCallClient() {
    // IN_SANDBOX has no representation in the Workflow.IssueStatusUpdated contract.
    underTest.publishStatusUpdated("ha-issue-key", IssueStatus.IN_SANDBOX);

    verifyNoInteractions(eventAsyncClient);
  }

  @Test
  void publishStatusUpdated_withMixOfInSandboxAndMappableStatuses_onlyPublishesTheMappableOne() {
    when(eventSourceBuilder.build(any(), any())).thenReturn(new EventSource("d", "s", null, null, null));
    when(eventAsyncClient.publishCrossDomainEvents(any())).thenReturn(CompletableFuture.completedFuture(null));

    underTest.publishStatusUpdated(Map.of("issue-1", IssueStatus.IN_SANDBOX, "issue-2", IssueStatus.FIXED));

    ArgumentCaptor<Collection<Event<?>>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(eventAsyncClient).publishCrossDomainEvents(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().iterator().next().payload()).isInstanceOfSatisfying(IssueStatusUpdatedEvent.class,
      payload -> assertThat(payload.getIssueId()).isEqualTo("issue-2"));
  }

  @Test
  void publishStatusUpdated_doesNotFailWhenClientThrowsSynchronously() {
    when(eventSourceBuilder.build(any(), any())).thenReturn(new EventSource("d", "s", null, null, null));
    when(eventAsyncClient.publishCrossDomainEvents(any())).thenThrow(new RuntimeException("boom"));

    underTest.publishStatusUpdated("ha-issue-key", IssueStatus.ACCEPTED);

    verify(eventAsyncClient).publishCrossDomainEvents(any());
  }

  @Test
  void publishStatusUpdated_doesNotFailWhenPublishCompletesExceptionally() {
    when(eventSourceBuilder.build(any(), any())).thenReturn(new EventSource("d", "s", null, null, null));
    when(eventAsyncClient.publishCrossDomainEvents(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

    underTest.publishStatusUpdated("ha-issue-key", IssueStatus.ACCEPTED);

    verify(eventAsyncClient).publishCrossDomainEvents(any());
  }
}
