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

import static org.assertj.core.api.Assertions.assertThat;

public class ChangesetTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void create_changeset() throws Exception {
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
  public void create_changeset_with_minimum_fields() throws Exception {
    Changeset underTest = Changeset.newChangesetBuilder()
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();

    assertThat(underTest.getAuthor()).isNull();
    assertThat(underTest.getDate()).isEqualTo(123456789L);
    assertThat(underTest.getRevision()).isEqualTo("rev-1");
  }

  @Test
  public void fail_with_NPE_when_setting_null_revision() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Revision cannot be null");

    Changeset.newChangesetBuilder().setRevision(null);
  }

  @Test
  public void fail_with_NPE_when_building_without_revision() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Revision cannot be null");

    Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .build();
  }

  @Test
  public void fail_with_NPE_when_setting_null_date() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Date cannot be null");

    Changeset.newChangesetBuilder().setDate(null);
  }

  @Test
  public void fail_with_NPE_when_building_without_date() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Date cannot be null");

    Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setRevision("rev-1")
      .build();
  }

  @Test
  public void test_to_string() throws Exception {
    Changeset underTest = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1")
      .build();

    assertThat(underTest.toString()).isEqualTo("Changeset{revision='rev-1', author='john', date=123456789}");
  }

  @Test
  public void test_equals_and_hash_code() throws Exception {
    Changeset.Builder changesetBuilder = Changeset.newChangesetBuilder()
      .setAuthor("john")
      .setDate(123456789L)
      .setRevision("rev-1");

    Changeset changeset = changesetBuilder.build();
    Changeset sameChangeset = changesetBuilder.build();

    Changeset anotherChangeset = Changeset.newChangesetBuilder()
      .setAuthor("henry")
      .setDate(1234567810L)
      .setRevision("rev-2")
      .build();

    assertThat(changeset).isEqualTo(sameChangeset);
    assertThat(changeset).isEqualTo(changeset);
    assertThat(changeset).isNotEqualTo(anotherChangeset);
  }
}
