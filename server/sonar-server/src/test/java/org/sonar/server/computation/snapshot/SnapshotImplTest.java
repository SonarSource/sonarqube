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

package org.sonar.server.computation.snapshot;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final long ID = 10;
  static final long CREATED_AT = 123456789L;

  @Test
  public void build_snapshot() throws Exception {
    Snapshot snapshot = new Snapshot.Builder()
      .setId(ID)
      .setCreatedAt(CREATED_AT)
      .build();

    assertThat(snapshot.getId()).isEqualTo(ID);
    assertThat(snapshot.getCreatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  public void fail_with_NPE_when_building_snapshot_without_id() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("id cannot be null");

    new Snapshot.Builder()
      .setCreatedAt(CREATED_AT)
      .build();
  }

  @Test
  public void fail_with_NPE_when_building_snapshot_without_created_at() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("createdAt cannot be null");

    new Snapshot.Builder()
      .setId(ID)
      .build();
  }

  @Test
  public void test_toString() throws Exception {
    assertThat(new Snapshot.Builder()
      .setId(ID)
      .setCreatedAt(CREATED_AT)
      .build().toString())
      .isEqualTo("SnapshotImpl{id=10, createdAt=123456789}");
  }

  @Test
  public void test_equals_and_hascode() throws Exception {
    Snapshot snapshot = new Snapshot.Builder()
      .setId(ID)
      .setCreatedAt(CREATED_AT)
      .build();
    Snapshot sameSnapshot = new Snapshot.Builder()
      .setId(ID)
      .setCreatedAt(CREATED_AT)
      .build();
    Snapshot otherSnapshot = new Snapshot.Builder()
      .setId(11L)
      .setCreatedAt(CREATED_AT)
      .build();

    assertThat(snapshot).isEqualTo(snapshot);
    assertThat(snapshot).isEqualTo(sameSnapshot);
    assertThat(snapshot).isNotEqualTo(otherSnapshot);
    assertThat(snapshot).isNotEqualTo(null);

    assertThat(snapshot.hashCode()).isEqualTo(snapshot.hashCode());
    assertThat(snapshot.hashCode()).isEqualTo(sameSnapshot.hashCode());
    assertThat(snapshot.hashCode()).isNotEqualTo(otherSnapshot.hashCode());
  }
}
