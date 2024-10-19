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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class ComponentsWithUnprocessedIssues {

  @CheckForNull
  private Set<String> uuids;

  public void setUuids(Set<String> uuids) {
    requireNonNull(uuids, "Uuids cannot be null");
    checkState(this.uuids == null, "Uuids have already been initialized");
    this.uuids = new HashSet<>(uuids);
  }

  public void remove(String uuid) {
    checkIssuesAreInitialized();
    uuids.remove(uuid);
  }

  public Set<String> getUuids() {
    checkIssuesAreInitialized();
    return uuids;
  }

  private void checkIssuesAreInitialized() {
    checkState(this.uuids != null, "Uuids have not been initialized yet");
  }

}
