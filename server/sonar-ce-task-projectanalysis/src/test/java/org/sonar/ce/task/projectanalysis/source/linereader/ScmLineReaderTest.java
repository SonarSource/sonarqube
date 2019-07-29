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
package org.sonar.ce.task.projectanalysis.source.linereader;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoImpl;
import org.sonar.db.protobuf.DbFileSources;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmLineReaderTest {

  @Test
  public void set_scm() {
    ScmInfo scmInfo = new ScmInfoImpl(new Changeset[] {
      Changeset.newChangesetBuilder()
        .setAuthor("john")
        .setDate(123_456_789L)
        .setRevision("rev-1")
        .build()});

    ScmLineReader lineScm = new ScmLineReader(scmInfo);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    assertThat(lineScm.read(lineBuilder)).isEmpty();

    assertThat(lineBuilder.getScmAuthor()).isEqualTo("john");
    assertThat(lineBuilder.getScmDate()).isEqualTo(123_456_789L);
    assertThat(lineBuilder.getScmRevision()).isEqualTo("rev-1");
  }

  @Test
  public void set_scm_with_minim_fields() {
    ScmInfo scmInfo = new ScmInfoImpl(new Changeset[] {
      Changeset.newChangesetBuilder()
        .setDate(123456789L)
        .build()});

    ScmLineReader lineScm = new ScmLineReader(scmInfo);

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    assertThat(lineScm.read(lineBuilder)).isEmpty();

    assertThat(lineBuilder.hasScmAuthor()).isFalse();
    assertThat(lineBuilder.getScmDate()).isEqualTo(123456789L);
    assertThat(lineBuilder.hasScmRevision()).isFalse();

  }

  @Test
  public void getLatestChange_returns_changeset_with_highest_date_of_read_lines() {
    long refDate = 123_456_789L;
    Changeset changeset0 = Changeset.newChangesetBuilder().setDate(refDate - 636).setRevision("rev-1").build();
    Changeset changeset1 = Changeset.newChangesetBuilder().setDate(refDate + 1).setRevision("rev-2").build();
    Changeset changeset2 = Changeset.newChangesetBuilder().setDate(refDate + 2).build();
    ScmInfo scmInfo = new ScmInfoImpl(setup8LinesChangeset(changeset0, changeset1, changeset2));

    ScmLineReader lineScm = new ScmLineReader(scmInfo);

    // before any line is read, the latest changes are null
    assertThat(lineScm.getLatestChange()).isNull();
    assertThat(lineScm.getLatestChangeWithRevision()).isNull();

    // read line 1, only one changeset => 0
    readLineAndAssertLatestChanges(lineScm, 1, changeset0, changeset0);

    // read line 2, latest changeset is 1
    readLineAndAssertLatestChanges(lineScm, 2, changeset1, changeset1);

    // read line 3, latest changeset is still 1
    readLineAndAssertLatestChanges(lineScm, 3, changeset1, changeset1);

    // read line 4, latest changeset is now 2
    readLineAndAssertLatestChanges(lineScm, 4, changeset2, changeset1);

    // read line 5 to 8, there will never be any changeset more recent than 2
    readLineAndAssertLatestChanges(lineScm, 5, changeset2, changeset1);
    readLineAndAssertLatestChanges(lineScm, 6, changeset2, changeset1);
    readLineAndAssertLatestChanges(lineScm, 7, changeset2, changeset1);
    readLineAndAssertLatestChanges(lineScm, 8, changeset2, changeset1);
  }

  private static Changeset[] setup8LinesChangeset(Changeset changeset0, Changeset changeset1, Changeset changeset2) {
    return new Changeset[] {
      changeset0,
      changeset1,
      changeset1,
      changeset2,
      changeset0,
      changeset1,
      changeset0,
      changeset0};
  }

  private void readLineAndAssertLatestChanges(ScmLineReader lineScm, int line, Changeset expectedChangeset, Changeset expectedChangesetWithRevision) {
    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(line);
    assertThat(lineScm.read(lineBuilder)).isEmpty();
    assertThat(lineScm.getLatestChange()).isSameAs(expectedChangeset);
    assertThat(lineScm.getLatestChangeWithRevision()).isSameAs(expectedChangesetWithRevision);

  }

}
