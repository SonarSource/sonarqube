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
class FPOrWontFixNotification extends Notification {
  private static final String KEY = "FPorWontFix";

  public enum FpOrWontFix {
    FP, WONT_FIX
  }

  private final Change change;
  private final SetMultimap<Project, ChangedIssue> changedIssues;
  private final FpOrWontFix resolution;

  public FPOrWontFixNotification(Change change, Collection<ChangedIssue> changedIssues, FpOrWontFix resolution) {
    super(KEY);
    this.changedIssues = changedIssues.stream().collect(unorderedIndex(ChangedIssue::getProject, t -> t));
    this.change = change;
    this.resolution = resolution;
  }

  public Change getChange() {
    return change;
  }

  public SetMultimap<Project, ChangedIssue> getChangedIssues() {
    return changedIssues;
  }

  public FpOrWontFix getResolution() {
    return resolution;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FPOrWontFixNotification that = (FPOrWontFixNotification) o;
    return Objects.equals(changedIssues, that.changedIssues) &&
      Objects.equals(change, that.change) &&
      resolution == that.resolution;
  }

  @Override
  public int hashCode() {
    return Objects.hash(changedIssues, change, resolution);
  }

  @Override
  public String toString() {
    return "FPOrWontFixNotification{" +
      "changedIssues=" + changedIssues +
      ", change=" + change +
      ", resolution=" + resolution +
      '}';
  }
}
