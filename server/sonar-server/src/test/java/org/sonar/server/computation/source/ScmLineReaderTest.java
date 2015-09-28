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

package org.sonar.server.computation.source;

import org.junit.Test;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.protobuf.DbFileSources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class ScmLineReaderTest {

  @Test
  public void set_scm() {
    BatchReport.Changesets scmReport = BatchReport.Changesets.newBuilder()
      .addChangeset(newChangeSetBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      .addChangesetIndexByLine(0)
      .build();

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    lineScm.read(lineBuilder);

    assertThat(lineBuilder.getScmAuthor()).isEqualTo("john");
    assertThat(lineBuilder.getScmDate()).isEqualTo(123456789L);
    assertThat(lineBuilder.getScmRevision()).isEqualTo("rev-1");
  }

  @Test
  public void set_only_author() {
    BatchReport.Changesets scmReport = BatchReport.Changesets.newBuilder()
      .addChangeset(newChangeSetBuilder()
        .setAuthor("john")
        .build())
      .addChangesetIndexByLine(0)
      .build();

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    lineScm.read(lineBuilder);

    assertThat(lineBuilder.getScmAuthor()).isEqualTo("john");
    assertThat(lineBuilder.hasScmDate()).isFalse();
    assertThat(lineBuilder.hasScmRevision()).isFalse();
  }

  @Test
  public void set_only_date() {
    BatchReport.Changesets scmReport = BatchReport.Changesets.newBuilder()
      .addChangeset(newChangeSetBuilder()
        .setDate(123456789L)
        .build())
      .addChangesetIndexByLine(0)
      .build();

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    lineScm.read(lineBuilder);

    assertThat(lineBuilder.hasScmAuthor()).isFalse();
    assertThat(lineBuilder.getScmDate()).isEqualTo(123456789L);
    assertThat(lineBuilder.hasScmRevision()).isFalse();
  }

  @Test
  public void set_only_revision() {
    BatchReport.Changesets scmReport = BatchReport.Changesets.newBuilder()
      .addChangeset(newChangeSetBuilder()
        .setRevision("rev-1")
        .build())
      .addChangesetIndexByLine(0)
      .build();

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    lineScm.read(lineBuilder);

    assertThat(lineBuilder.hasScmAuthor()).isFalse();
    assertThat(lineBuilder.hasScmDate()).isFalse();
    assertThat(lineBuilder.getScmRevision()).isEqualTo("rev-1");
  }

  @Test
  public void fail_when_changeset_is_empty() {
    BatchReport.Changesets scmReport = BatchReport.Changesets.newBuilder()
      .addChangeset(newChangeSetBuilder()
        .build())
      .addChangesetIndexByLine(0)
      .build();

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    try {
      lineScm.read(lineBuilder);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("A changeset must contain at least one of : author, revision or date");
    }
  }

  @Test
  public void getLatestChange_returns_changeset_with_highest_date_of_read_lines() {
    long refDate = 123456789L;
    BatchReport.Changesets.Changeset changeset0 = newChangeSetBuilder().setDate(refDate - 636).build();
    BatchReport.Changesets.Changeset changeset1 = newChangeSetBuilder().setDate(refDate + 1).build();
    BatchReport.Changesets.Changeset changeset2 = newChangeSetBuilder().setDate(refDate + 2).build();
    BatchReport.Changesets scmReport = setup8LinesChangeset(changeset0, changeset1, changeset2);

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    // before any line is read, the latest change is null
    assertThat(lineScm.getLatestChange()).isNull();

    // read line 1, only one changeset => 0
    readLineAndAssertLatestChangeDate(lineScm, 1, changeset0);

    // read line 2, latest changeset is 1
    readLineAndAssertLatestChangeDate(lineScm, 2, changeset1);

    // read line 3, latest changeset is still 1
    readLineAndAssertLatestChangeDate(lineScm, 3, changeset1);

    // read line 4, latest changeset is now 2
    readLineAndAssertLatestChangeDate(lineScm, 4, changeset2);

    // read line 5 to 8, there will never be any changeset more recent than 2
    readLineAndAssertLatestChangeDate(lineScm, 5, changeset2);
    readLineAndAssertLatestChangeDate(lineScm, 6, changeset2);
    readLineAndAssertLatestChangeDate(lineScm, 7, changeset2);
    readLineAndAssertLatestChangeDate(lineScm, 8, changeset2);
  }

  @Test
  public void getLatestChange_returns_first_changeset_when_none_have_dates() {
    BatchReport.Changesets.Changeset changeset0 = newChangeSetBuilder().setRevision("0").build();
    BatchReport.Changesets.Changeset changeset1 = newChangeSetBuilder().setRevision("1").build();
    BatchReport.Changesets.Changeset changeset2 = newChangeSetBuilder().setRevision("2").build();
    BatchReport.Changesets scmReport = setup8LinesChangeset(changeset0, changeset1, changeset2);

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    // before any line is read, the latest change is null
    assertThat(lineScm.getLatestChange()).isNull();

    // read lines 1 to 8, no date => changeset 0
    readLineAndAssertLatestChangeDate(lineScm, 1, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 2, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 3, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 4, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 5, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 6, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 7, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 8, changeset0);
  }

  @Test
  public void getLatestChange_returns_first_changeset_when_the_first_one_has_no_date() {
    BatchReport.Changesets.Changeset changeset0 = newChangeSetBuilder().setRevision("0").build();
    BatchReport.Changesets.Changeset changeset1 = newChangeSetBuilder().setRevision("1").setDate(95454154L).build();
    BatchReport.Changesets.Changeset changeset2 = newChangeSetBuilder().setRevision("2").setDate(9654545444L).build();
    BatchReport.Changesets scmReport = setup8LinesChangeset(changeset0, changeset1, changeset2);

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    // before any line is read, the latest change is null
    assertThat(lineScm.getLatestChange()).isNull();

    // read lines 1 to 8, first encountered changeset has no date => changeset 0
    readLineAndAssertLatestChangeDate(lineScm, 1, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 2, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 3, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 4, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 5, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 6, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 7, changeset0);
    readLineAndAssertLatestChangeDate(lineScm, 8, changeset0);
  }

  private static BatchReport.Changesets setup8LinesChangeset(BatchReport.Changesets.Changeset changeset0,
    BatchReport.Changesets.Changeset changeset1,
    BatchReport.Changesets.Changeset changeset2) {
    return BatchReport.Changesets.newBuilder()
      .addChangeset(changeset0)
      .addChangeset(changeset1)
      .addChangeset(changeset2)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(2)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .build();
  }

  private void readLineAndAssertLatestChangeDate(ScmLineReader lineScm, int line, BatchReport.Changesets.Changeset expectedChangeset) {
    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(line);
    lineScm.read(lineBuilder);
    assertThat(lineScm.getLatestChange()).isSameAs(expectedChangeset);
  }

  private static BatchReport.Changesets.Changeset.Builder newChangeSetBuilder() {
    return BatchReport.Changesets.Changeset.newBuilder();
  }

}
