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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.computation.component.Component;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.isEmpty;
import static java.lang.String.format;

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
    Iterable<Changeset> lineChangesets = from(lines)
      .transform(lineToChangeset)
      .filter(notNull())
      .toList();
    if (isEmpty(lineChangesets)) {
      return Optional.absent();
    }
    checkState(!lineToChangeset.isEncounteredLineWithoutScmInfo(),
      format("Partial scm information stored in DB for component '%s'. Not all lines have SCM info. Can not proceed", component));
    return Optional.<ScmInfo>of(new DbScmInfo(new ScmInfoImpl(lineChangesets)));
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

    @Override
    @Nullable
    public Changeset apply(@Nonnull DbFileSources.Line input) {
      if (input.hasScmRevision() || input.hasScmAuthor() || input.hasScmDate()) {
        return Changeset.newChangesetBuilder()
          .setRevision(input.getScmRevision())
          .setAuthor(input.getScmAuthor())
          .setDate(input.getScmDate())
          .build();
      }

      this.encounteredLineWithoutScmInfo = true;
      return null;
    }

    public boolean isEncounteredLineWithoutScmInfo() {
      return encounteredLineWithoutScmInfo;
    }
  }
}
