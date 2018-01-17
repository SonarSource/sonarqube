/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.scm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangesetTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_changeset() {
    Changeset underTest = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();

    assertThat(underTest.getAuthor()).isEqualTo("john");
    assertThat(underTest.getDate()).isEqualTo(123456789L);
    assertThat(underTest.getRevision()).isEqualTo("rev-1");
  }

  @Test
  public void create_changeset_with_minimum_fields() {
    Changeset underTest = Changeset.newChangesetBuilder()
      .setDate(123456789L)
      .build();

    assertThat(underTest.getAuthor()).isNull();
    assertThat(underTest.getDate()).isEqualTo(123456789L);
    assertThat(underTest.getRevision()).isNull();
  }

  @Test
  public void fail_with_NPE_when_setting_null_date() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Date cannot be null");

    Changeset.newChangesetBuilder().setDate(null);
  }

  @Test
  public void fail_with_NPE_when_building_without_date() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Date cannot be null");

    Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setRevision("rev-1")
      .build();
  }

  @Test
  public void test_to_string() {
    Changeset underTest = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();

    assertThat(underTest.toString()).isEqualTo("Changeset{revision='rev-1', author='john', date=123456789}");
  }

  @Test
  public void equals_and_hashcode_are_based_on_all_fields() throws Exception {
    Changeset.Builder changesetBuilder = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1");

    Changeset changeset = changesetBuilder.build();
    Changeset sameChangeset = changesetBuilder.build();

    Changeset anotherChangesetWithSameRevision = Changeset.newChangesetBuilder()
      .setAuthor("henry")
      .setDate(1234567810L)
      .setRevision("rev-1")
      .build();

    Changeset anotherChangeset = Changeset.newChangesetBuilder()
      .setAuthor("henry")
      .setDate(996L)
      .setRevision("rev-2")
      .build();

    assertThat(changeset).isEqualTo(changeset);
    assertThat(changeset).isEqualTo(sameChangeset);
    assertThat(changeset).isNotEqualTo(anotherChangesetWithSameRevision);
    assertThat(changeset).isNotEqualTo(anotherChangeset);

    assertThat(changeset.hashCode()).isEqualTo(changeset.hashCode());
    assertThat(changeset.hashCode()).isEqualTo(sameChangeset.hashCode());
    assertThat(changeset.hashCode()).isNotEqualTo(anotherChangesetWithSameRevision.hashCode());
    assertThat(changeset.hashCode()).isNotEqualTo(anotherChangeset.hashCode());
  }
}
