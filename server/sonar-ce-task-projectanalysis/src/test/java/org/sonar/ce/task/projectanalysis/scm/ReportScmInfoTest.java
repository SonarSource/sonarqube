/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.junit.Test;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportScmInfoTest {

  static final int FILE_REF = 1;

  @Test
  public void create_scm_info_with_some_changesets() {
    ScmInfo scmInfo = ReportScmInfo.create(ScannerReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
        .setAuthor("henry")
        .setDate(1234567810L)
        .setRevision("rev-2")
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .build());

    assertThat(scmInfo.getAllChangesets()).hasSize(4);
  }

  @Test
  public void return_changeset_for_a_given_line() {
    ScmInfo scmInfo = ReportScmInfo.create(ScannerReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
        .setAuthor("henry")
        .setDate(1234567810L)
        .setRevision("rev-2")
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(0)
      .build());

    assertThat(scmInfo.getAllChangesets()).hasSize(4);

    Changeset changeset = scmInfo.getChangesetForLine(4);
    assertThat(changeset.getAuthor()).isEqualTo("john");
    assertThat(changeset.getDate()).isEqualTo(123456789L);
    assertThat(changeset.getRevision()).isEqualTo("rev-1");
  }

  @Test
  public void return_latest_changeset() {
    ScmInfo scmInfo = ReportScmInfo.create(ScannerReport.Changesets.newBuilder()
      .setComponentRef(FILE_REF)
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
        .setAuthor("john")
        .setDate(123456789L)
        .setRevision("rev-1")
        .build())
      // Older changeset
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
        .setAuthor("henry")
        .setDate(1234567810L)
        .setRevision("rev-2")
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(0)
      .build());

    Changeset latestChangeset = scmInfo.getLatestChangeset();
    assertThat(latestChangeset.getAuthor()).isEqualTo("henry");
    assertThat(latestChangeset.getDate()).isEqualTo(1234567810L);
    assertThat(latestChangeset.getRevision()).isEqualTo("rev-2");
  }

  @Test
  public void fail_with_ISE_when_no_changeset() {
    assertThatThrownBy(() -> ReportScmInfo.create(ScannerReport.Changesets.newBuilder().build()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("ScmInfo cannot be empty");
  }

  @Test
  public void fail_with_NPE_when_report_is_null() {
    assertThatThrownBy(() -> ReportScmInfo.create(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void fail_with_ISE_when_changeset_has_no_revision() {
    assertThatThrownBy(() -> {
      ReportScmInfo.create(ScannerReport.Changesets.newBuilder()
        .setComponentRef(FILE_REF)
        .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
          .setAuthor("john")
          .setDate(123456789L)
          .build())
        .addChangesetIndexByLine(0)
        .build());
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Changeset on line 1 must have a revision");
  }

  @Test
  public void fail_with_ISE_when_changeset_has_no_date() {
    assertThatThrownBy(() -> {
      ReportScmInfo.create(ScannerReport.Changesets.newBuilder()
        .setComponentRef(FILE_REF)
        .addChangeset(ScannerReport.Changesets.Changeset.newBuilder()
          .setAuthor("john")
          .setRevision("rev-1")
          .build())
        .addChangesetIndexByLine(0)
        .build());
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Changeset on line 1 must have a date");
  }

}
