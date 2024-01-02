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
package org.sonar.core.util.issue;

import java.io.Serializable;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class IssueChangedEvent implements Serializable {
  private static final String EVENT = "IssueChanged";

  private final String projectKey;
  private final Issue[] issues;
  @CheckForNull
  private final Boolean resolved;
  @CheckForNull
  private final String userSeverity;
  @CheckForNull
  private final String userType;

  public IssueChangedEvent(String projectKey, Issue[] issues, @Nullable Boolean resolved, @Nullable String userSeverity,
    @Nullable String userType) {
    if (issues.length == 0) {
      throw new IllegalArgumentException("Can't create IssueChangedEvent without any issues that have changed");
    }
    this.projectKey = projectKey;
    this.issues = issues;
    this.resolved = resolved;
    this.userSeverity = userSeverity;
    this.userType = userType;
  }

  public String getEvent() {
    return EVENT;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public Issue[] getIssues() {
    return issues;
  }

  @CheckForNull
  public Boolean getResolved() {
    return resolved;
  }

  @CheckForNull
  public String getUserSeverity() {
    return userSeverity;
  }

  @CheckForNull
  public String getUserType() {
    return userType;
  }

}
