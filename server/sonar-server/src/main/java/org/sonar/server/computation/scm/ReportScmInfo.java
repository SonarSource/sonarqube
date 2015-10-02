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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import org.sonar.batch.protocol.output.BatchReport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

class ReportScmInfo implements ScmInfo {
  private final ScmInfo delegate;

  ReportScmInfo(BatchReport.Changesets changesets) {
    requireNonNull(changesets);
    this.delegate = convertToScmInfo(changesets);
  }

  private ScmInfo convertToScmInfo(final BatchReport.Changesets changesets) {
    return new ScmInfoImpl(
      from(new IntRangeIterable(changesets.getChangesetIndexByLineCount()))
        .transform(new LineIndexToChangeset(changesets)));
  }

  private static Changeset convert(BatchReport.Changesets.Changeset changeset) {
    return Changeset.newChangesetBuilder()
      .setRevision(changeset.hasRevision() ? changeset.getRevision() : null)
      .setAuthor(changeset.hasAuthor() ? changeset.getAuthor() : null)
      .setDate(changeset.hasDate() ? changeset.getDate() : null)
      .build();
  }

  @Override
  public Optional<Changeset> getLatestChangeset() {
    return this.delegate.getLatestChangeset();
  }

  @Override
  public Optional<Changeset> getForLine(int lineNumber) {
    return this.delegate.getForLine(lineNumber);
  }

  @Override
  public Iterable<Changeset> getForLines() {
    return this.delegate.getForLines();
  }

  private static class LineIndexToChangeset implements Function<Integer, Changeset> {
    private final BatchReport.Changesets changesets;
    private Map<Integer, Changeset> changeSetByIndex;

    public LineIndexToChangeset(BatchReport.Changesets changesets) {
      this.changesets = changesets;
      changeSetByIndex = new HashMap<>(changesets.getChangesetCount());
    }

    @Override
    @Nonnull
    public Changeset apply(@Nonnull Integer input) {
      int changesetIndex = changesets.getChangesetIndexByLine(input - 1);
      if (changeSetByIndex.containsKey(changesetIndex)) {
        return changeSetByIndex.get(changesetIndex);
      }
      Changeset res = convert(changesets.getChangeset(changesetIndex));
      changeSetByIndex.put(changesetIndex, res);
      return res;
    }
  }

  /**
   * A simple Iterable which generate integer from 0 (included) to a specific value (excluded).
   */
  private static final class IntRangeIterable implements Iterable<Integer> {
    private final int max;

    private IntRangeIterable(int max) {
      checkArgument(max >= 0, "Max value must be >= 0");
      this.max = max;
    }

    @Override
    public Iterator<Integer> iterator() {
      return new IntRangeIterator(max);
    }
  }

  private static class IntRangeIterator implements Iterator<Integer> {
    private final int max;

    private int current = 0;

    public IntRangeIterator(int max) {
      this.max = max;
    }

    @Override
    public boolean hasNext() {
      return current < max;
    }

    @Override
    public Integer next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      current++;
      return current;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove cannot be called");
    }
  }
}
