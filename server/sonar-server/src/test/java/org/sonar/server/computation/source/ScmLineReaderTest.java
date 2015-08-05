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
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
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
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
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
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
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
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
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
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .build())
      .addChangesetIndexByLine(0)
      .build();

    ScmLineReader lineScm = new ScmLineReader(scmReport);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    try {
      lineScm.read(lineBuilder);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("A changeset must contains at least one of : author, revision or date");
    }
  }

}
