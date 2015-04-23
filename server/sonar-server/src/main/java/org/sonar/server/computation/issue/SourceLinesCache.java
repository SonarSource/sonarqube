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
package org.sonar.server.computation.issue;

import org.apache.commons.lang.StringUtils;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Changesets.Changeset;
import org.sonar.batch.protocol.output.BatchReport.Changesets.Changeset.Builder;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache of the lines of the currently processed file. Only a <strong>single</strong> file
 * is kept in memory at a time. Data is loaded <strong>on demand</strong> (to avoid non necessary
 * loading).<br />
 * It relies on:
 * <ul>
 *   <li>the SCM information sent in the report for modified files</li>
 *   <li>the source line index for non-modified files</li>
 * </ul>
 *
 */
public class SourceLinesCache {

  private final SourceLineIndex index;
  private BatchReportReader reportReader;

  private boolean loaded = false;
  private BatchReport.Changesets scm;
  private String currentFileUuid;
  private Integer currentFileReportRef;

  private long lastCommitDate = 0L;
  private String lastCommitAuthor = null;

  public SourceLinesCache(SourceLineIndex index) {
    this.index = index;
  }

  /**
   * Marks the currently processed component
   */
  void init(String fileUuid, @Nullable Integer fileReportRef, BatchReportReader thisReportReader) {
    loaded = false;
    currentFileUuid = fileUuid;
    currentFileReportRef = fileReportRef;
    lastCommitDate = 0L;
    lastCommitAuthor = null;
    reportReader = thisReportReader;
    clear();
  }

  /**
   * Last committer of the line, can be null.
   */
  @CheckForNull
  public String lineAuthor(@Nullable Integer lineNumber) {
    loadIfNeeded();

    if (lineNumber == null) {
      // issue on file, approximately estimate that author is the last committer on the file
      return lastCommitAuthor;
    }
    String author = null;
    if (lineNumber <= scm.getChangesetIndexByLineCount()) {
      BatchReport.Changesets.Changeset changeset = scm.getChangeset(scm.getChangesetIndexByLine(lineNumber-1));
      author = changeset.hasAuthor() ? changeset.getAuthor() : null;
    }

    return StringUtils.defaultIfEmpty(author, lastCommitAuthor);
  }

  private void loadIfNeeded() {
    checkState();

    if (!loaded) {
      scm = loadScmFromReport();
      loaded = scm != null;
    }

    if (!loaded) {
      scm = loadLinesFromIndexAndBuildScm();
      loaded = true;
    }

    computeLastCommitDateAndAuthor();
  }

  private BatchReport.Changesets loadScmFromReport() {
    return reportReader.readChangesets(currentFileReportRef);
  }

  private BatchReport.Changesets loadLinesFromIndexAndBuildScm() {
    List<SourceLineDoc> lines = index.getLines(currentFileUuid);
    Map<String, BatchReport.Changesets.Changeset> changesetByRevision = new HashMap<>();
    BatchReport.Changesets.Builder scmBuilder = BatchReport.Changesets.newBuilder()
      .setComponentRef(currentFileReportRef);
    for (SourceLineDoc sourceLine : lines) {
      String scmRevision = sourceLine.scmRevision();
      if (scmRevision == null || changesetByRevision.get(scmRevision) == null) {
        Builder changeSetBuilder = BatchReport.Changesets.Changeset.newBuilder();
        String scmAuthor = sourceLine.scmAuthor();
        if (scmAuthor != null) {
          changeSetBuilder.setAuthor(scmAuthor);
        }
        Date scmDate = sourceLine.scmDate();
        if (scmDate != null) {
          changeSetBuilder.setDate(scmDate.getTime());
        }
        if (scmRevision != null) {
          changeSetBuilder.setRevision(scmRevision);
        }

        Changeset changeset = changeSetBuilder.build();
        scmBuilder.addChangeset(changeset);
        scmBuilder.addChangesetIndexByLine(scmBuilder.getChangesetCount() - 1);
        if (scmRevision != null) {
          changesetByRevision.put(scmRevision, changeset);
        }
      } else {
        scmBuilder.addChangesetIndexByLine(scmBuilder.getChangesetList().indexOf(changesetByRevision.get(scmRevision)));
      }
    }
    return scmBuilder.build();
  }

  private void computeLastCommitDateAndAuthor() {
    for (BatchReport.Changesets.Changeset changeset : scm.getChangesetList()) {
      if (changeset.hasAuthor() && changeset.hasDate() && changeset.getDate() > lastCommitDate) {
        lastCommitDate = changeset.getDate();
        lastCommitAuthor = changeset.getAuthor();
      }
    }
  }

  private void checkState() {
    if (currentFileReportRef == null) {
      throw new IllegalStateException("Report component reference must not be null to use the cache");
    }
  }

  /**
   * Makes cache eligible to GC
   */
  public void clear() {
    scm = null;
  }
}
