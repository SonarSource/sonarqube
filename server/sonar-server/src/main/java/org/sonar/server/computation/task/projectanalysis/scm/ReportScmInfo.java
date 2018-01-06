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

import com.google.common.base.Function;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * ScmInfo implementation based on the changeset information from the Report
 */
@Immutable
class ReportScmInfo implements ScmInfo {
  private final ScmInfo delegate;

  ReportScmInfo(ScannerReport.Changesets changesets) {
    requireNonNull(changesets);
    this.delegate = convertToScmInfo(changesets);
  }

  private static ScmInfo convertToScmInfo(ScannerReport.Changesets changesets) {
    return new ScmInfoImpl(
      from(new IntRangeIterable(changesets.getChangesetIndexByLineCount()))
        .transform(new LineIndexToChangeset(changesets)));
  }

  @Override
  public Changeset getLatestChangeset() {
    return this.delegate.getLatestChangeset();
  }

  @Override
  public Changeset getChangesetForLine(int lineNumber) {
    return this.delegate.getChangesetForLine(lineNumber);
  }

  @Override
  public boolean hasChangesetForLine(int lineNumber) {
    return delegate.hasChangesetForLine(lineNumber);
  }

  @Override
  public Iterable<Changeset> getAllChangesets() {
    return this.delegate.getAllChangesets();
  }

  private static class LineIndexToChangeset implements Function<Integer, Changeset> {
    private final ScannerReport.Changesets changesets;
    private final Map<Integer, Changeset> changesetCache;
    private final Changeset.Builder builder = Changeset.newChangesetBuilder();

    public LineIndexToChangeset(ScannerReport.Changesets changesets) {
      this.changesets = changesets;
      changesetCache = new HashMap<>(changesets.getChangesetCount());
    }

    @Override
    @Nonnull
    public Changeset apply(@Nonnull Integer lineNumber) {
      int changesetIndex = changesets.getChangesetIndexByLine(lineNumber - 1);
      Changeset changeset = changesetCache.get(changesetIndex);
      if (changeset != null) {
        return changeset;
      }
      Changeset res = convert(changesets.getChangeset(changesetIndex), lineNumber);
      changesetCache.put(changesetIndex, res);
      return res;
    }

    private Changeset convert(ScannerReport.Changesets.Changeset changeset, int line) {
      checkState(isNotEmpty(changeset.getRevision()), "Changeset on line %s must have a revision", line);
      checkState(changeset.getDate() != 0, "Changeset on line %s must have a date", line);
      return builder
        .setRevision(changeset.getRevision())
        .setAuthor(isNotEmpty(changeset.getAuthor()) ? changeset.getAuthor() : null)
        .setDate(changeset.getDate())
        .build();
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
