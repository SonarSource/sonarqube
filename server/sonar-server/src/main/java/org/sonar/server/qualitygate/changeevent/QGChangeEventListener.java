/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.sonar.api.rules.RuleType;

public interface QGChangeEventListener {
  /**
   * @deprecated use {{@link #onIssueChanges(QGChangeEvent, Set)}} instead
   */
  // TODO remove this method and turn default method #onIssueChanges(QGChangeEvent, Set) into an interface method when
  // support for #onIssueChanges(QGChangeEvent, Set) have been merged into sonar-branch's master
  @Deprecated
  default void onChanges(Trigger trigger, Collection<QGChangeEvent> changeEvents) {
    // do nothing
  }

  /**
   * Called consequently to a change done on one or more issue of a given project.
   *
   * @param qualityGateEvent can not be {@code null}
   * @param changedIssues can not be {@code null} nor empty
   */
  default void onIssueChanges(QGChangeEvent qualityGateEvent, Set<ChangedIssue> changedIssues) {
    onChanges(Trigger.ISSUE_CHANGE, Collections.singleton(qualityGateEvent));
  }

  interface ChangedIssue {
    String getKey();

    Status getStatus();

    RuleType getType();
  }

  enum Status {
    OPEN, CONFIRMED, REOPENED, RESOLVED, CLOSED
  }
}
