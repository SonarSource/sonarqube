/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener.ChangedIssue;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.String.format;

/**
 * Broadcast a given collection of {@link QGChangeEvent} for a specific trigger to all the registered
 * {@link QGChangeEventListener} in the ioc container.
 *
 * This class ensures that an {@link Exception} occurring calling one of the {@link QGChangeEventListener} doesn't
 * prevent from calling the others.
 */
public class QGChangeEventListenersImpl implements QGChangeEventListeners {
  private static final Logger LOG = LoggerFactory.getLogger(QGChangeEventListenersImpl.class);

  private final Set<QGChangeEventListener> listeners;

  public QGChangeEventListenersImpl(Set<QGChangeEventListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void broadcastOnIssueChange(List<DefaultIssue> issues, Collection<QGChangeEvent> changeEvents, boolean fromAlm) {
    if (listeners.isEmpty() || issues.isEmpty() || changeEvents.isEmpty()) {
      return;
    }

    try {
      broadcastChangeEventsToBranches(issues, changeEvents, fromAlm);
    } catch (Error e) {
      LOG.warn(format("Broadcasting to listeners failed for %s events", changeEvents.size()), e);
    }
  }

  @Override
  public void broadcastOnAnyChange(Collection<QGChangeEvent> changeEvents, boolean fromAlm) {
    if (listeners.isEmpty() || changeEvents.isEmpty()) {
      return;
    }

    try {
      for (var changeEvent : changeEvents) {
        listeners.forEach(listener -> broadcastChangeEventToListener(Set.of(), changeEvent, listener));
      }
    } catch (Error e) {
      LOG.warn(format("Broadcasting to listeners failed for %s events", changeEvents.size()), e);
    }
  }

  private void broadcastChangeEventsToBranches(List<DefaultIssue> issues, Collection<QGChangeEvent> changeEvents, boolean fromAlm) {
    Multimap<String, QGChangeEvent> eventsByBranchUuid = changeEvents.stream()
      .collect(MoreCollectors.index(qgChangeEvent -> qgChangeEvent.getBranch().getUuid()));

    Multimap<String, DefaultIssue> issueByBranchUuid = issues.stream()
      .collect(MoreCollectors.index(DefaultIssue::projectUuid));

    issueByBranchUuid.asMap().forEach(
      (branchUuid, branchIssues) -> broadcastChangeEventsToBranch(branchIssues, eventsByBranchUuid.get(branchUuid), fromAlm));
  }

  private void broadcastChangeEventsToBranch(Collection<DefaultIssue> branchIssues, Collection<QGChangeEvent> branchQgChangeEvents, boolean fromAlm) {
    Set<ChangedIssue> changedIssues = toChangedIssues(branchIssues, fromAlm);
    branchQgChangeEvents.forEach(changeEvent -> broadcastChangeEventToListeners(changedIssues, changeEvent));
  }

  private static ImmutableSet<ChangedIssue> toChangedIssues(Collection<DefaultIssue> defaultIssues, boolean fromAlm) {
    return defaultIssues.stream()
      .map(defaultIssue -> new ChangedIssueImpl(defaultIssue, fromAlm))
      .collect(toImmutableSet());
  }

  private void broadcastChangeEventToListeners(Set<ChangedIssue> changedIssues, QGChangeEvent changeEvent) {
    listeners.forEach(listener -> broadcastChangeEventToListener(changedIssues, changeEvent, listener));
  }

  private static void broadcastChangeEventToListener(Set<ChangedIssue> changedIssues, QGChangeEvent changeEvent, QGChangeEventListener listener) {
    try {
      LOG.trace("calling onChange() on listener {} for events {}...", listener.getClass().getName(), changeEvent);
      listener.onIssueChanges(changeEvent, changedIssues);
    } catch (Exception e) {
      LOG.warn(format("onChange() call failed on listener %s for events %s", listener.getClass().getName(), changeEvent), e);
    }
  }

}
