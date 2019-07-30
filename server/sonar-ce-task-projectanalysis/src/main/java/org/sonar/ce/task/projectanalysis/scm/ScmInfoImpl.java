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
package org.sonar.ce.task.projectanalysis.scm;

import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.utils.Preconditions;

@Immutable
public class ScmInfoImpl implements ScmInfo {
  private final Changeset latestChangeset;
  private final Changeset[] lineChangesets;

  public ScmInfoImpl(Changeset[] lineChangesets) {
    Preconditions.checkNotNull(lineChangesets);
    Preconditions.checkState(lineChangesets.length > 0, "ScmInfo cannot be empty");
    this.lineChangesets = lineChangesets;
    this.latestChangeset = computeLatestChangeset(lineChangesets);
  }

  private static Changeset computeLatestChangeset(Changeset[] lineChangesets) {
    return Arrays.stream(lineChangesets).max(Comparator.comparingLong(Changeset::getDate))
      .orElseThrow(() -> new IllegalStateException("Expecting at least one Changeset to be present"));
  }

  @Override
  public Changeset getLatestChangeset() {
    return latestChangeset;
  }

  @Override
  public Changeset getChangesetForLine(int lineNumber) {
    Changeset changeset = lineChangesets[lineNumber - 1];
    if (changeset != null) {
      return changeset;
    }
    throw new IllegalArgumentException("There's no changeset on line " + lineNumber);
  }

  @Override
  public boolean hasChangesetForLine(int lineNumber) {
    return lineNumber - 1 < lineChangesets.length && lineChangesets[lineNumber - 1] != null;
  }

  @Override
  public Changeset[] getAllChangesets() {
    return lineChangesets;
  }

  @Override
  public String toString() {
    return "ScmInfoImpl{" +
      "latestChangeset=" + latestChangeset +
      ", lineChangesets=" + lineChangesets +
      '}';
  }
}
