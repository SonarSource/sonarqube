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
package org.sonar.server.qualitygate.changeevent;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;

import static org.sonar.api.rules.RuleType.VULNERABILITY;

@ServerSide
public interface QGChangeEventListener {
  /**
   * Called consequently to a change done on one or more issue of a given project.
   *
   * @param qualityGateEvent can not be {@code null}
   * @param changedIssues can not be {@code null} nor empty
   */
  void onIssueChanges(QGChangeEvent qualityGateEvent, Set<ChangedIssue> changedIssues);

  interface ChangedIssue {

    String getKey();

    Status getStatus();

    RuleType getType();

    Map<SoftwareQuality, Severity> getImpacts();

    String getSeverity();

    default boolean isNotClosed() {
      return !Status.CLOSED_STATUSES.contains(getStatus());
    }

    default boolean isVulnerability() {
      return getType() == VULNERABILITY;
    }

    default boolean fromAlm() {
      return false;
    }
  }

  enum Status {
    OPEN,
    CONFIRMED,
    REOPENED,
    RESOLVED_FP,
    RESOLVED_WF,
    RESOLVED_FIXED,
    TO_REVIEW,
    IN_REVIEW,
    REVIEWED;

    protected static final Set<Status> CLOSED_STATUSES = EnumSet.of(CONFIRMED, RESOLVED_FIXED, RESOLVED_FP, RESOLVED_WF);
  }

}
