/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.purge;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PurgeableSnapshotDtoTest {
  @Test
  public void testEquals() {
    PurgeableSnapshotDto dto1 = new PurgeableSnapshotDto().setSnapshotId(3L);
    PurgeableSnapshotDto dto2 = new PurgeableSnapshotDto().setSnapshotId(4L);
    assertThat(dto1.equals(dto2), is(false));
    assertThat(dto2.equals(dto1), is(false));
    assertThat(dto1.equals(dto1), is(true));
    assertThat(dto1.equals(new PurgeableSnapshotDto().setSnapshotId(3L)), is(true));
    assertThat(dto1.equals("bi_bop_a_lou_la"), is(false));
    assertThat(dto1.equals(null), is(false));
  }

  @Test
  public void testHasCode() {
    PurgeableSnapshotDto dto = new PurgeableSnapshotDto().setSnapshotId(3L);
    assertThat(dto.hashCode(), is(dto.hashCode()));

    // no id
    dto = new PurgeableSnapshotDto();
    assertThat(dto.hashCode(), is(dto.hashCode()));
  }

  @Test
  public void testToString() {
    PurgeableSnapshotDto dto = new PurgeableSnapshotDto().setSnapshotId(3L);
    assertThat(dto.toString().length(), greaterThan(0));
  }
}
