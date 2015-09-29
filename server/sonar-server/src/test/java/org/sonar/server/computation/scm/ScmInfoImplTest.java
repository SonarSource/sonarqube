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

import com.google.common.collect.Lists;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class ScmInfoImplTest {

  static final Changeset CHANGESET_1 = Changeset.newChangesetBuilder()
    .setAuthor("john")
    .setDate(123456789L)
    .setRevision("rev-1")
    .build();

  static final Changeset CHANGESET_2 = Changeset.newChangesetBuilder()
    .setAuthor("henry")
    .setDate(1234567810L)
    .setRevision("rev-2")
    .build();


  @Test
  public void get_all_changesets() throws Exception {
    ScmInfo scmInfo = createScmInfoWithTwoChangestOnFourLines();

    assertThat(scmInfo.getForLines()).containsOnly(CHANGESET_1, CHANGESET_2, CHANGESET_1, CHANGESET_1);
  }

  @Test
  public void get_no_changeset() throws Exception {
    ScmInfo scmInfo = new ScmInfoImpl(Lists.<Changeset>newArrayList());

    assertThat(scmInfo.getForLines()).isEmpty();
  }

  @Test
  public void get_latest_changeset() throws Exception {
    ScmInfo scmInfo = createScmInfoWithTwoChangestOnFourLines();

    assertThat(scmInfo.getLatestChangeset().get()).isEqualTo(CHANGESET_2);
  }

  @Test
  public void return_no_latest_changeset_when_changeset_has_no_date() throws Exception {
    Changeset changeset1 = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(null)
      .setRevision("rev-1")
      .build();
    Changeset changeset2 = Changeset.newChangesetBuilder()
      .setAuthor("henry")
      .setDate(null)
      .setRevision("rev-2")
      .build();

    ScmInfo scmInfo = new ScmInfoImpl(newArrayList(changeset1, changeset2, changeset1, changeset1));

    assertThat(scmInfo.getLatestChangeset()).isAbsent();
  }

  @Test
  public void get_changeset_for_given_line() throws Exception {
    ScmInfo scmInfo = createScmInfoWithTwoChangestOnFourLines();

    assertThat(scmInfo.getForLine(1).get()).isEqualTo(CHANGESET_1);
    assertThat(scmInfo.getForLine(2).get()).isEqualTo(CHANGESET_2);
    assertThat(scmInfo.getForLine(3).get()).isEqualTo(CHANGESET_1);
    assertThat(scmInfo.getForLine(4).get()).isEqualTo(CHANGESET_1);
  }

  @Test
  public void return_no_changeset_when_line_is_bigger_than_changetset_size() throws Exception {
    ScmInfo scmInfo = createScmInfoWithTwoChangestOnFourLines();

    assertThat(scmInfo.getForLine(5)).isAbsent();
  }

  @Test
  public void test_to_string() throws Exception {
    ScmInfo scmInfo = createScmInfoWithTwoChangestOnFourLines();

    assertThat(scmInfo.toString()).isEqualTo("ScmInfoImpl{" +
      "latestChangeset=Changeset{revision='rev-2', author='henry', date=1234567810}, " +
      "lineChangesets=[" +
      "Changeset{revision='rev-1', author='john', date=123456789}, " +
      "Changeset{revision='rev-2', author='henry', date=1234567810}, " +
      "Changeset{revision='rev-1', author='john', date=123456789}, " +
      "Changeset{revision='rev-1', author='john', date=123456789}" +
      "]}");
  }

  private static ScmInfo createScmInfoWithTwoChangestOnFourLines() {
    Changeset changeset1 = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();
    // Latest changeset
    Changeset changeset2 = Changeset.newChangesetBuilder()
      .setAuthor("henry")
      .setDate(1234567810L)
      .setRevision("rev-2")
      .build();

    ScmInfo scmInfo = new ScmInfoImpl(newArrayList(changeset1, changeset2, changeset1, changeset1));
    return scmInfo;
  }
}
