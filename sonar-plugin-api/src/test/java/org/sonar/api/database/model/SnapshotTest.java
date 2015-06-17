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
package org.sonar.api.database.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SnapshotTest {

  @Test
  public void testGetDate() {
    Snapshot snapshot = new Snapshot();
    assertNull(snapshot.getCreatedAtMs());

    Long now = System.currentTimeMillis();
    snapshot.setCreatedAtMs(now);
    assertEquals(now, snapshot.getCreatedAtMs());
  }

  @Test
  public void testGetVersion() {
    Snapshot snapshot = new Snapshot();
    assertNull(snapshot.getVersion());

    snapshot.setVersion("1.0");
    assertEquals("1.0", snapshot.getVersion());
  }

  @Test
  public void testGetStatus() {
    Snapshot snapshot = new Snapshot();
    assertNotNull(snapshot.getStatus());
    assertEquals(Snapshot.STATUS_UNPROCESSED, snapshot.getStatus());
  }

  @Test
  public void testGetLast() {
    Snapshot snapshot = new Snapshot();
    assertNotNull(snapshot.getLast());
    assertEquals(Boolean.FALSE, snapshot.getLast());
  }

}
