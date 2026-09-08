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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.IssueStatus;
import org.sonarsource.issueprocessing.events.workflow.generated.IssueStatusUpdatedEvent;
import org.sonarsource.sonarqube.events.api.BaseEvent;
import org.sonarsource.sonarqube.events.api.Event;
import org.sonarsource.sonarqube.events.api.EventAsyncClient;
import org.sonarsource.sonarqube.events.api.EventMetadata;
import org.sonarsource.sonarqube.events.api.EventSourceBuilder;

/**
 * Publishes {@code Workflow.IssueStatusUpdated} events whenever a user changes the status of an issue, so that
 * systems tracking that issue's lifecycle (e.g. an issue-producing agent) can observe the triage decision.
 */
public class IssueEventsPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(IssueEventsPublisher.class);

  static final String EVENT_TYPE = "Workflow.IssueStatusUpdated";
  static final String EVENT_VERSION = "1.0";
  static final String SOURCE_DOMAIN = "Workflow";
  static final String SOURCE_SERVICE = "IssueEventsPublisher";

  private final EventAsyncClient eventAsyncClient;
  private final EventSourceBuilder eventSourceBuilder;

  public IssueEventsPublisher(EventAsyncClient eventAsyncClient, EventSourceBuilder eventSourceBuilder) {
    this.eventAsyncClient = eventAsyncClient;
    this.eventSourceBuilder = eventSourceBuilder;
  }

  public void publishStatusUpdated(String issueKey, IssueStatus status) {
    publishStatusUpdated(Map.of(issueKey, status));
  }

  public void publishStatusUpdated(Map<String, IssueStatus> statusByIssueKey) {
    if (statusByIssueKey.isEmpty()) {
      return;
    }
    try {
      List<Event<?>> events = statusByIssueKey.entrySet().stream()
        .<Event<?>>map(entry -> toEvent(entry.getKey(), entry.getValue()))
        .filter(Objects::nonNull)
        .toList();
      if (events.isEmpty()) {
        return;
      }
      eventAsyncClient.publishCrossDomainEvents(events)
        .whenComplete((ignored, throwable) -> {
          if (throwable != null) {
            // Publishing this event must never fail the user's transition.
            LOG.warn("Failed to publish issue status updated event(s)", throwable);
          }
        });
    } catch (RuntimeException e) {
      LOG.warn("Failed to publish issue status updated event(s)", e);
    }
  }

  @CheckForNull
  private Event<?> toEvent(String issueKey, IssueStatus status) {
    org.sonarsource.issueprocessing.events.workflow.generated.IssueStatus generatedStatus = toGeneratedStatus(status);
    if (generatedStatus == null) {
      return null;
    }
    IssueStatusUpdatedEvent payload = IssueStatusUpdatedEvent.builder()
      .withIssueId(issueKey)
      .withStatus(generatedStatus)
      .build();
    return new BaseEvent<>(new EventMetadata(eventSourceBuilder.build(SOURCE_DOMAIN, SOURCE_SERVICE), EVENT_TYPE, EVENT_VERSION), payload);
  }

  /**
   * The Workflow.IssueStatusUpdated contract only covers the 5 statuses a user-triggered transition can end in;
   * {@link IssueStatus#IN_SANDBOX} has no wire representation and is not surfaced. Written as an exhaustive switch
   * (no {@code default}) so that a future addition to {@link IssueStatus} fails to compile here instead of throwing
   * at runtime.
   */
  @CheckForNull
  private static org.sonarsource.issueprocessing.events.workflow.generated.IssueStatus toGeneratedStatus(IssueStatus status) {
    return switch (status) {
      case OPEN -> org.sonarsource.issueprocessing.events.workflow.generated.IssueStatus.OPEN;
      case CONFIRMED -> org.sonarsource.issueprocessing.events.workflow.generated.IssueStatus.CONFIRMED;
      case ACCEPTED -> org.sonarsource.issueprocessing.events.workflow.generated.IssueStatus.ACCEPTED;
      case FALSE_POSITIVE -> org.sonarsource.issueprocessing.events.workflow.generated.IssueStatus.FALSE_POSITIVE;
      case FIXED -> org.sonarsource.issueprocessing.events.workflow.generated.IssueStatus.FIXED;
      case IN_SANDBOX -> null;
    };
  }
}
