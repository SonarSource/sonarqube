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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.protobuf.DbFileSources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.source.index.FileSourceTesting.newFakeData;

public class DbScmInfoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_scm_info_with_some_changesets() {
    ScmInfo scmInfo = DbScmInfo.create(newFakeData(10).build().getLinesList(), 10, "hash").get();

    assertThat(scmInfo.getAllChangesets()).hasSize(10);
  }

  @Test
  public void return_changeset_for_a_given_line() {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    addLine(fileDataBuilder, 1, "john", 123456789L, "rev-1");
    addLine(fileDataBuilder, 2, "henry", 1234567810L, "rev-2");
    addLine(fileDataBuilder, 3, "henry", 1234567810L, "rev-2");
    addLine(fileDataBuilder, 4, "john", 123456789L, "rev-1");
    fileDataBuilder.build();

    ScmInfo scmInfo = DbScmInfo.create(fileDataBuilder.getLinesList(), 4, "hash").get();

    assertThat(scmInfo.getAllChangesets()).hasSize(4);

    Changeset changeset = scmInfo.getChangesetForLine(4);
    assertThat(changeset.getAuthor()).isEqualTo("john");
    assertThat(changeset.getDate()).isEqualTo(123456789L);
    assertThat(changeset.getRevision()).isEqualTo("rev-1");
  }

  @Test
  public void return_same_changeset_objects_for_lines_with_same_fields() {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    fileDataBuilder.addLinesBuilder().setScmRevision("rev").setScmDate(65L).setLine(1);
    fileDataBuilder.addLinesBuilder().setScmRevision("rev2").setScmDate(6541L).setLine(2);
    fileDataBuilder.addLinesBuilder().setScmRevision("rev1").setScmDate(6541L).setLine(3);
    fileDataBuilder.addLinesBuilder().setScmRevision("rev").setScmDate(65L).setLine(4);

    ScmInfo scmInfo = DbScmInfo.create(fileDataBuilder.getLinesList(), 4, "hash").get();

    assertThat(scmInfo.getAllChangesets()).hasSize(4);

    assertThat(scmInfo.getChangesetForLine(1)).isSameAs(scmInfo.getChangesetForLine(4));
  }

  @Test
  public void return_latest_changeset() {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    addLine(fileDataBuilder, 1, "john", 123456789L, "rev-1");
    // Older changeset
    addLine(fileDataBuilder, 2, "henry", 1234567810L, "rev-2");
    addLine(fileDataBuilder, 3, "john", 123456789L, "rev-1");
    fileDataBuilder.build();

    ScmInfo scmInfo = DbScmInfo.create(fileDataBuilder.getLinesList(), 3, "hash").get();

    Changeset latestChangeset = scmInfo.getLatestChangeset();
    assertThat(latestChangeset.getAuthor()).isEqualTo("henry");
    assertThat(latestChangeset.getDate()).isEqualTo(1234567810L);
    assertThat(latestChangeset.getRevision()).isEqualTo("rev-2");
  }

  @Test
  public void return_absent_dsm_info_when_no_changeset() {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    fileDataBuilder.addLinesBuilder().setLine(1);

    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 1, "hash")).isNotPresent();
  }

  @Test
  public void should_support_some_lines_not_having_scm_info() {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    fileDataBuilder.addLinesBuilder().setScmRevision("rev").setScmDate(543L).setLine(1);
    fileDataBuilder.addLinesBuilder().setLine(2);
    fileDataBuilder.build();

    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().getAllChangesets()).hasSize(2);
    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().hasChangesetForLine(1)).isTrue();
    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().hasChangesetForLine(2)).isFalse();
  }

  @Test
  public void filter_out_entries_without_date() {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    fileDataBuilder.addLinesBuilder().setScmRevision("rev").setScmDate(555L).setLine(1);
    fileDataBuilder.addLinesBuilder().setScmRevision("rev-1").setLine(2);
    fileDataBuilder.build();

    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().getAllChangesets()).hasSize(2);
    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().getChangesetForLine(1).getRevision()).isEqualTo("rev");
    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().hasChangesetForLine(2)).isFalse();

  }

  @Test
  public void should_support_having_no_author() {
    DbFileSources.Data.Builder fileDataBuilder = DbFileSources.Data.newBuilder();
    // gets filtered out
    fileDataBuilder.addLinesBuilder().setScmAuthor("John").setLine(1);
    fileDataBuilder.addLinesBuilder().setScmRevision("rev").setScmDate(555L).setLine(2);
    fileDataBuilder.build();

    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().getAllChangesets()).hasSize(2);
    assertThat(DbScmInfo.create(fileDataBuilder.getLinesList(), 2, "hash").get().getChangesetForLine(2).getAuthor()).isNull();
  }

  private static void addLine(DbFileSources.Data.Builder dataBuilder, Integer line, String author, Long date, String revision) {
    dataBuilder.addLinesBuilder()
      .setLine(line)
      .setScmAuthor(author)
      .setScmDate(date)
      .setScmRevision(revision);
  }

}
