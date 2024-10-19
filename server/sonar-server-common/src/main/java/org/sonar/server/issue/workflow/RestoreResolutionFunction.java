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
package org.sonar.server.issue.workflow;

import java.util.Comparator;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;

enum RestoreResolutionFunction implements Function {
  INSTANCE;

  @Override
  public void execute(Context context) {
    DefaultIssue defaultIssue = (DefaultIssue) context.issue();
    String previousResolution = defaultIssue.changes().stream()
      // exclude current change (if any)
      .filter(change -> change != defaultIssue.currentChange())
      .filter(change -> change.creationDate() != null)
      .sorted(Comparator.comparing(FieldDiffs::creationDate).reversed())
      .map(this::parse)
      .filter(Objects::nonNull)
      .filter(StatusAndResolutionDiffs::hasResolution)
      .findFirst()
      .map(t -> t.newStatusClosed ? t.oldResolution : t.newResolution)
      .orElse(null);
    context.setResolution(previousResolution);
  }

  @CheckForNull
  private StatusAndResolutionDiffs parse(FieldDiffs fieldDiffs) {
    FieldDiffs.Diff status = fieldDiffs.get("status");
    if (status == null) {
      return null;
    }
    FieldDiffs.Diff resolution = fieldDiffs.get("resolution");
    if (resolution == null) {
      return new StatusAndResolutionDiffs(Issue.STATUS_CLOSED.equals(status.newValue()), null, null);
    }
    return new StatusAndResolutionDiffs(Issue.STATUS_CLOSED.equals(status.newValue()), (String) resolution.oldValue(), (String) resolution.newValue());
  }

  private static class StatusAndResolutionDiffs {
    private final boolean newStatusClosed;
    private final String oldResolution;
    private final String newResolution;

    private StatusAndResolutionDiffs(boolean newStatusClosed, @Nullable String oldResolution, @Nullable String newResolution) {
      this.newStatusClosed = newStatusClosed;
      this.oldResolution = emptyToNull(oldResolution);
      this.newResolution = emptyToNull(newResolution);
    }

    private static String emptyToNull(@Nullable String str) {
      if (str == null || str.isEmpty()) {
        return null;
      }
      return str;
    }

    boolean hasResolution() {
      return oldResolution != null || newResolution != null;
    }
  }

}
