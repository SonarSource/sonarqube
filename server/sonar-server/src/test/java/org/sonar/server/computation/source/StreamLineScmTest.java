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
import org.sonar.server.source.db.FileSourceDb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class StreamLineScmTest {

  @Test
  public void set_scm() throws Exception {
    BatchReport.Scm scmReport = BatchReport.Scm.newBuilder()
      .addChangeset(BatchReport.Scm.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789)
        .setRevision("rev-1")
        .build())
      .addChangesetIndexByLine(0)
      .build();

    StreamLineScm lineScm = new StreamLineScm(scmReport);

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder();
    lineScm.readLine(1, lineBuilder);

    assertThat(lineBuilder.getScmAuthor()).isEqualTo("john");
    assertThat(lineBuilder.getScmDate()).isEqualTo(123456789);
    assertThat(lineBuilder.getScmRevision()).isEqualTo("rev-1");
  }

  @Test
  public void set_only_author() throws Exception {
    BatchReport.Scm scmReport = BatchReport.Scm.newBuilder()
      .addChangeset(BatchReport.Scm.Changeset.newBuilder()
        .setAuthor("john")
        .build())
      .addChangesetIndexByLine(0)
      .build();

    StreamLineScm lineScm = new StreamLineScm(scmReport);

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder();
    lineScm.readLine(1, lineBuilder);

    assertThat(lineBuilder.getScmAuthor()).isEqualTo("john");
    assertThat(lineBuilder.hasScmDate()).isFalse();
    assertThat(lineBuilder.hasScmRevision()).isFalse();
  }

  @Test
  public void set_only_date() throws Exception {
    BatchReport.Scm scmReport = BatchReport.Scm.newBuilder()
      .addChangeset(BatchReport.Scm.Changeset.newBuilder()
        .setDate(123456789)
        .build())
      .addChangesetIndexByLine(0)
      .build();

    StreamLineScm lineScm = new StreamLineScm(scmReport);

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder();
    lineScm.readLine(1, lineBuilder);

    assertThat(lineBuilder.hasScmAuthor()).isFalse();
    assertThat(lineBuilder.getScmDate()).isEqualTo(123456789);
    assertThat(lineBuilder.hasScmRevision()).isFalse();
  }

  @Test
  public void set_only_revision() throws Exception {
    BatchReport.Scm scmReport = BatchReport.Scm.newBuilder()
      .addChangeset(BatchReport.Scm.Changeset.newBuilder()
        .setRevision("rev-1")
        .build())
      .addChangesetIndexByLine(0)
      .build();

    StreamLineScm lineScm = new StreamLineScm(scmReport);

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder();
    lineScm.readLine(1, lineBuilder);

    assertThat(lineBuilder.hasScmAuthor()).isFalse();
    assertThat(lineBuilder.hasScmDate()).isFalse();
    assertThat(lineBuilder.getScmRevision()).isEqualTo("rev-1");
  }

  @Test
  public void fail_when_changeset_is_empty() throws Exception {
    BatchReport.Scm scmReport = BatchReport.Scm.newBuilder()
      .addChangeset(BatchReport.Scm.Changeset.newBuilder()
        .build())
      .addChangesetIndexByLine(0)
      .build();

    StreamLineScm lineScm = new StreamLineScm(scmReport);

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder();
    try {
      lineScm.readLine(1, lineBuilder);
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("A changeset must contains at least one of : author, revision or date");
    }
  }

}
