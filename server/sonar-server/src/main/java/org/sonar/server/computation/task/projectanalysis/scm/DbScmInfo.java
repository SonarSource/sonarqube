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
package org.sonar.server.computation.task.projectanalysis.scm;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static com.google.common.base.Preconditions.checkState;

/**
 * ScmInfo implementation based on the lines stored in DB
 */
@Immutable
class DbScmInfo implements ScmInfo {

  private final ScmInfo delegate;

  private DbScmInfo(ScmInfo delegate) {
    this.delegate = delegate;
  }

  static Optional<ScmInfo> create(Component component, Iterable<DbFileSources.Line> lines) {
    LineToChangeset lineToChangeset = new LineToChangeset();
    List<Changeset> lineChangesets = StreamSupport.stream(lines.spliterator(), false)
      .map(lineToChangeset)
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toList());
    if (lineChangesets.isEmpty()) {
      return Optional.absent();
    }
    checkState(!lineToChangeset.isEncounteredLineWithoutScmInfo(),
      "Partial scm information stored in DB for component '%s'. Not all lines have SCM info. Can not proceed", component);
    return Optional.of(new DbScmInfo(new ScmInfoImpl(lineChangesets)));
  }

  @Override
  public Changeset getLatestChangeset() {
    return delegate.getLatestChangeset();
  }

  @Override
  public Changeset getChangesetForLine(int lineNumber) {
    return delegate.getChangesetForLine(lineNumber);
  }

  @Override
  public boolean hasChangesetForLine(int lineNumber) {
    return delegate.hasChangesetForLine(lineNumber);
  }

  @Override
  public Iterable<Changeset> getAllChangesets() {
    return delegate.getAllChangesets();
  }

  /**
   * Transforms {@link org.sonar.db.protobuf.DbFileSources.Line} into {@link Changeset} and keep a flag if it encountered
   * at least one which did not have any SCM information.
   */
  private static class LineToChangeset implements Function<DbFileSources.Line, Changeset> {
    private boolean encounteredLineWithoutScmInfo = false;
    private final Map<String, Changeset> cache = new HashMap<>();
    private final Changeset.Builder builder = Changeset.newChangesetBuilder();

    @Override
    @Nullable
    public Changeset apply(@Nonnull DbFileSources.Line input) {
      if (input.hasScmRevision() && input.hasScmDate()) {
        String revision = input.getScmRevision();
        return cache.computeIfAbsent(revision, k -> builder
          .setRevision(revision)
          .setAuthor(input.hasScmAuthor() ? input.getScmAuthor() : null)
          .setDate(input.getScmDate())
          .build());
      }

      this.encounteredLineWithoutScmInfo = true;
      return null;
    }

    boolean isEncounteredLineWithoutScmInfo() {
      return encounteredLineWithoutScmInfo;
    }
  }
}
