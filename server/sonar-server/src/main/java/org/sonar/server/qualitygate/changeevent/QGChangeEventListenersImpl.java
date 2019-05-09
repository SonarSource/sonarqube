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
package org.sonar.server.qualitygate.changeevent;

import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener.ChangedIssue;

import static java.lang.String.format;
import static org.sonar.core.util.stream.MoreCollectors.toSet;

/**
 * Broadcast a given collection of {@link QGChangeEvent} for a specific trigger to all the registered
 * {@link QGChangeEventListener} in Pico.
 *
 * This class ensures that an {@link Exception} occurring calling one of the {@link QGChangeEventListener} doesn't
 * prevent from calling the others.
 */
public class QGChangeEventListenersImpl implements QGChangeEventListeners {
  private static final Logger LOG = Loggers.get(QGChangeEventListenersImpl.class);

  private final QGChangeEventListener[] listeners;

  /**
   * Used by Pico when there is no QGChangeEventListener instance in container.
   */
  public QGChangeEventListenersImpl() {
    this.listeners = new QGChangeEventListener[0];
  }

  public QGChangeEventListenersImpl(QGChangeEventListener[] listeners) {
    this.listeners = listeners;
  }

  @Override
  public void broadcastOnIssueChange(List<DefaultIssue> issues, Collection<QGChangeEvent> changeEvents) {
    if (listeners.length == 0 || issues.isEmpty() || changeEvents.isEmpty()) {
      return;
    }

    try {
      Multimap<String, QGChangeEvent> eventsByComponentUuid = changeEvents.stream()
        .collect(MoreCollectors.index(t -> t.getProject().uuid()));
      Multimap<String, DefaultIssue> issueByComponentUuid = issues.stream()
        .collect(MoreCollectors.index(DefaultIssue::projectUuid));

      issueByComponentUuid.asMap()
        .forEach((componentUuid, value) -> {
          Collection<QGChangeEvent> qgChangeEvents = eventsByComponentUuid.get(componentUuid);
          if (!qgChangeEvents.isEmpty()) {
            Set<ChangedIssue> changedIssues = value.stream()
              .map(ChangedIssueImpl::new)
              .collect(toSet());
            qgChangeEvents
              .forEach(changeEvent -> Arrays.stream(listeners)
                .forEach(listener -> broadcastTo(changedIssues, changeEvent, listener)));
          }
        });
    } catch (Error e) {
      LOG.warn(format("Broadcasting to listeners failed for %s events", changeEvents.size()), e);
    }
  }

  private static void broadcastTo(Set<ChangedIssue> changedIssues, QGChangeEvent changeEvent, QGChangeEventListener listener) {
    try {
      LOG.trace("calling onChange() on listener {} for events {}...", listener.getClass().getName(), changeEvent);
      listener.onIssueChanges(changeEvent, changedIssues);
    } catch (Exception e) {
      LOG.warn(format("onChange() call failed on listener %s for events %s", listener.getClass().getName(), changeEvent), e);
    }
  }

  static class ChangedIssueImpl implements ChangedIssue {
    private final String key;
    private final QGChangeEventListener.Status status;
    private final RuleType type;
    private final String severity;

    ChangedIssueImpl(DefaultIssue issue) {
      this.key = issue.key();
      this.status = statusOf(issue);
      this.type = issue.type();
      this.severity = issue.severity();
    }

    static QGChangeEventListener.Status statusOf(DefaultIssue issue) {
      switch (issue.status()) {
        case Issue.STATUS_OPEN:
          return QGChangeEventListener.Status.OPEN;
        case Issue.STATUS_CONFIRMED:
          return QGChangeEventListener.Status.CONFIRMED;
        case Issue.STATUS_REOPENED:
          return QGChangeEventListener.Status.REOPENED;
        case Issue.STATUS_TO_REVIEW:
          return QGChangeEventListener.Status.TO_REVIEW;
        case Issue.STATUS_IN_REVIEW:
          return QGChangeEventListener.Status.IN_REVIEW;
        case Issue.STATUS_REVIEWED:
          return QGChangeEventListener.Status.REVIEWED;
        case Issue.STATUS_RESOLVED:
          return statusOfResolved(issue);
        default:
          throw new IllegalStateException("Unexpected status: " + issue.status());
      }
    }

    private static QGChangeEventListener.Status statusOfResolved(DefaultIssue issue) {
      String resolution = issue.resolution();
      Objects.requireNonNull(resolution, "A resolved issue should have a resolution");
      switch (resolution) {
        case Issue.RESOLUTION_FALSE_POSITIVE:
          return QGChangeEventListener.Status.RESOLVED_FP;
        case Issue.RESOLUTION_WONT_FIX:
          return QGChangeEventListener.Status.RESOLVED_WF;
        case Issue.RESOLUTION_FIXED:
          return QGChangeEventListener.Status.RESOLVED_FIXED;
        default:
          throw new IllegalStateException("Unexpected resolution for a resolved issue: " + resolution);
      }
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public QGChangeEventListener.Status getStatus() {
      return status;
    }

    @Override
    public RuleType getType() {
      return type;
    }

    @Override
    public String getSeverity() {
      return severity;
    }

    @Override
    public String toString() {
      return "ChangedIssueImpl{" +
        "key='" + key + '\'' +
        ", status=" + status +
        ", type=" + type +
        ", severity=" + severity +
        '}';
    }
  }

}
