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
package org.sonar.server.issue.workflow.issue;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowEntity;

/**
 * The common part between issues and security hotspots
 */
public class IssueWorkflowEntityAdapter implements SecurityHotspotWorkflowEntity {

  protected final DefaultIssue issue;

  public IssueWorkflowEntityAdapter(DefaultIssue issue) {
    this.issue = issue;
  }

  @Override
  public boolean isBeingClosed() {
    return issue.isBeingClosed();
  }

  @Override
  public boolean hasAnyResolution(String... resolutions) {
    return issue.resolution() != null && Set.of(resolutions).contains(issue.resolution());
  }

  @Override
  public boolean previousStatusWas(String expectedPreviousStatus) {
    Optional<String> lastPreviousStatus = issue.changes().stream()
      // exclude current change (if any)
      .filter(change -> change != issue.currentChange())
      .filter(change -> change.creationDate() != null)
      .sorted(Comparator.comparing(FieldDiffs::creationDate).reversed())
      .map(change -> change.get("status"))
      .filter(Objects::nonNull)
      .findFirst()
      .map(t -> (String) t.oldValue());

    return lastPreviousStatus.filter(expectedPreviousStatus::equals).isPresent();
  }

}
