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
package org.sonar.server.issue.notification;

import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Objects;
import org.sonar.api.notifications.Notification;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Change;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;

import static org.sonar.core.util.stream.MoreCollectors.unorderedIndex;

/**
 * This notification is never serialized to DB.
 * <p>
 * It is derived from {@link IssuesChangesNotification} by
 * {@link FPOrWontFixNotificationHandler} and extends {@link Notification} only to comply with
 * {@link org.sonar.server.issue.notification.EmailTemplate#format(Notification)} API.
 */
class ChangesOnMyIssuesNotification extends Notification {
  private final Change change;
  private final SetMultimap<Project, ChangedIssue> changedIssues;

  public ChangesOnMyIssuesNotification(Change change, Collection<ChangedIssue> changedIssues) {
    super("ChangesOnMyIssues");
    this.change = change;
    this.changedIssues = changedIssues.stream().collect(unorderedIndex(ChangedIssue::getProject, t -> t));
  }

  public Change getChange() {
    return change;
  }

  public SetMultimap<Project, ChangedIssue> getChangedIssues() {
    return changedIssues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChangesOnMyIssuesNotification that = (ChangesOnMyIssuesNotification) o;
    return Objects.equals(change, that.change) &&
      Objects.equals(changedIssues, that.changedIssues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(change, changedIssues);
  }
}
