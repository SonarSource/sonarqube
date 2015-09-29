/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.scm;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

@Immutable
public class ScmInfoImpl implements ScmInfo {

  @CheckForNull
  private final Changeset latestChangeset;
  private final Changeset[] lineChangesets;

  public ScmInfoImpl(Iterable<Changeset> lineChangesets) {
    this.lineChangesets = from(lineChangesets).toArray(Changeset.class);
    this.latestChangeset = computeLatestChangeset(lineChangesets);
  }

  private static Changeset computeLatestChangeset(Iterable<Changeset> lineChangesets) {
    Changeset latestChangeset = null;
    for (Changeset lineChangeset : FluentIterable.from(lineChangesets).filter(WithDate.INSTANCE)) {
      if (latestChangeset == null) {
        latestChangeset = lineChangeset;
      } else {
        if (lineChangeset.getDate() > latestChangeset.getDate()) {
          latestChangeset = lineChangeset;
        }
      }
    }
    return latestChangeset;
  }

  @Override
  public Optional<Changeset> getLatestChangeset() {
    return fromNullable(latestChangeset);
  }

  @Override
  public Optional<Changeset> getForLine(int lineNumber) {
    if (lineNumber <= lineChangesets.length) {
      return Optional.of(lineChangesets[lineNumber - 1]);
    }
    return Optional.absent();
  }

  @Override
  public Iterable<Changeset> getForLines() {
    return asList(lineChangesets);
  }

  @Override
  public String toString() {
    return "ScmInfoImpl{" +
      "latestChangeset=" + latestChangeset +
      ", lineChangesets=" + Arrays.toString(lineChangesets) +
      '}';
  }

  private enum WithDate implements Predicate<Changeset> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Changeset input) {
      return input.getDate() != null;
    }
  }
}
