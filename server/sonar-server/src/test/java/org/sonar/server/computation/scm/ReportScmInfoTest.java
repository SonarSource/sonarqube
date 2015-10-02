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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.batch.protocol.output.BatchReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class ReportScmInfoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final int FILE_REF = 1;

  @Test
  public void create_scm_info_with_some_changesets() throws Exception {
    ScmInfo scmInfo = new ReportScmInfo(BatchReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("henry")
        .setDate(1234567810L)
        .setRevision("rev-2")
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .build());

    assertThat(scmInfo.getForLines()).hasSize(4);
  }

  @Test
  public void return_changeset_for_a_given_line() throws Exception {
    ScmInfo scmInfo = new ReportScmInfo(BatchReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("henry")
        .setDate(1234567810L)
        .setRevision("rev-2")
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(0)
      .build());

    assertThat(scmInfo.getForLines()).hasSize(4);

    assertThat(scmInfo.getForLine(4)).isPresent();
    Changeset changeset = scmInfo.getForLine(4).get();
    assertThat(changeset.getAuthor()).isEqualTo("john");
    assertThat(changeset.getDate()).isEqualTo(123456789L);
    assertThat(changeset.getRevision()).isEqualTo("rev-1");
  }

  @Test
  public void return_latest_changeset() throws Exception {
    ScmInfo scmInfo = new ReportScmInfo(BatchReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      // Older changeset
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("henry")
        .setDate(1234567810L)
        .setRevision("rev-2")
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(0)
      .build());

    assertThat(scmInfo.getLatestChangeset()).isPresent();

    Changeset latestChangeset = scmInfo.getLatestChangeset().get();
    assertThat(latestChangeset.getAuthor()).isEqualTo("henry");
    assertThat(latestChangeset.getDate()).isEqualTo(1234567810L);
    assertThat(latestChangeset.getRevision()).isEqualTo("rev-2");
  }

  @Test
  public void create_empty_scm_info() throws Exception {
    ScmInfo scmInfo = new ReportScmInfo(BatchReport.Changesets.newBuilder().build());

    assertThat(scmInfo.getForLines()).isEmpty();
  }

  @Test
  public void fail_with_NPE_when_report_is_null() throws Exception {
    thrown.expect(NullPointerException.class);

    new ReportScmInfo(null);
  }

}
