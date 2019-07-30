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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.concurrent.Immutable;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.base.Preconditions.checkState;
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
    Changeset[] lineChangesets = new Changeset[changesets.getChangesetIndexByLineCount()];
    LineIndexToChangeset lineIndexToChangeset = new LineIndexToChangeset(changesets);

    for (int i = 0; i < changesets.getChangesetIndexByLineCount(); i++) {
      lineChangesets[i] = lineIndexToChangeset.apply(i);
    }

    return new ScmInfoImpl(lineChangesets);
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
  public Changeset[] getAllChangesets() {
    return this.delegate.getAllChangesets();
  }

  private static class LineIndexToChangeset implements Function<Integer, Changeset> {
    private final ScannerReport.Changesets changesets;
    private final Map<Integer, Changeset> changesetCache;
    private final Changeset.Builder builder = Changeset.newChangesetBuilder();

    public LineIndexToChangeset(ScannerReport.Changesets changesets) {
      this.changesets = changesets;
      this.changesetCache = new HashMap<>(changesets.getChangesetCount());
    }

    @Override
    public Changeset apply(Integer lineNumber) {
      int changesetIndex = changesets.getChangesetIndexByLine(lineNumber);
      return changesetCache.computeIfAbsent(changesetIndex, idx -> convert(changesets.getChangeset(changesetIndex), lineNumber));
    }

    private Changeset convert(ScannerReport.Changesets.Changeset changeset, int line) {
      checkState(isNotEmpty(changeset.getRevision()), "Changeset on line %s must have a revision", line);
      checkState(changeset.getDate() != 0, "Changeset on line %s must have a date", line);
      return builder
        .setRevision(changeset.getRevision().intern())
        .setAuthor(isNotEmpty(changeset.getAuthor()) ? changeset.getAuthor().intern() : null)
        .setDate(changeset.getDate())
        .build();
    }
  }
}
