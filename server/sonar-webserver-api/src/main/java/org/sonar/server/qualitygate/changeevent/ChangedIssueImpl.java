/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Map;
import java.util.Objects;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;

class ChangedIssueImpl implements QGChangeEventListener.ChangedIssue {
  private final String key;
  private final QGChangeEventListener.Status status;
  private final Map<SoftwareQuality, Severity> impacts;
  private final RuleType type;
  private final String severity;
  private final boolean fromAlm;

  ChangedIssueImpl(DefaultIssue issue) {
    this(issue, false);
  }

  ChangedIssueImpl(DefaultIssue issue, boolean fromAlm) {
    this.key = issue.key();
    this.status = statusOf(issue);
    this.type = issue.type();
    this.severity = issue.severity();
    this.impacts = issue.impacts();
    this.fromAlm = fromAlm;
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
  public Map<SoftwareQuality, Severity> getImpacts() {
    return impacts;
  }

  @Override
  public String getSeverity() {
    return severity;
  }

  @Override
  public boolean fromAlm() {
    return fromAlm;
  }

  @Override
  public String toString() {
    return "ChangedIssueImpl{" +
      "key='" + key + '\'' +
      ", status=" + status +
      ", type=" + type +
      ", severity=" + severity +
      ", fromAlm=" + fromAlm +
      '}';
  }
}
